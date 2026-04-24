package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisRequestResult;
import com.hufs.capstone.backend.link.application.event.LinkProcessingRequestedEvent;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkAnalysisRequest;
import com.hufs.capstone.backend.link.domain.repository.LinkAnalysisRequestRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkAnalysisRequestWriteService {

	private final LinkRepository linkRepository;
	private final LinkAnalysisRequestRepository linkAnalysisRequestRepository;
	private final RoomAccessService roomAccessService;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public LinkAnalysisRequestResult requestWithinWriteTransaction(
			LinkUrlNormalizer.NormalizedUrl normalizedUrl,
			String roomId,
			Long userId,
			String source
	) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		AnalysisTarget target = findOrCreateLink(normalizedUrl);
		boolean createdRequest = findOrCreateAnalysisRequest(target.link(), room, userId, source);
		boolean recoveredDispatchFailed = recoverDispatchFailedForManualRetry(target.link());

		publishProcessingRequestedEventIfNeeded(
				target.createdNewLink() || recoveredDispatchFailed,
				target.link(),
				normalizedUrl.normalizedUrl(),
				room.getPublicId(),
				source
		);

		return LinkAnalysisRequestResult.from(target.link(), createdRequest);
	}

	private AnalysisTarget findOrCreateLink(LinkUrlNormalizer.NormalizedUrl normalizedUrl) {
		Link existing = linkRepository.findByNormalizedUrl(normalizedUrl.normalizedUrl()).orElse(null);
		if (existing != null) {
			return new AnalysisTarget(existing, false);
		}
		Link created = persistNewLink(normalizedUrl);
		return new AnalysisTarget(created, true);
	}

	private Link persistNewLink(LinkUrlNormalizer.NormalizedUrl normalizedUrl) {
		Link newLink = Link.registerPending(normalizedUrl.originalUrl(), normalizedUrl.normalizedUrl());
		try {
			return linkRepository.saveAndFlush(newLink);
		} catch (DataIntegrityViolationException ex) {
			throw new LinkDuplicateRaceException(normalizedUrl.normalizedUrl(), ex);
		} catch (DataAccessException ex) {
			log.error("링크 분석 대상 저장에 실패했습니다. normalizedUrl={}", normalizedUrl.normalizedUrl(), ex);
			throw new BusinessException(ErrorCode.E500_INTERNAL, "링크 분석 대상 저장에 실패했습니다.", ex);
		}
	}

	private boolean findOrCreateAnalysisRequest(Link link, Room room, Long userId, String source) {
		if (linkAnalysisRequestRepository.findByRoomAndLinkId(room, link.getId()).isPresent()) {
			return false;
		}

		try {
			linkAnalysisRequestRepository.saveAndFlush(LinkAnalysisRequest.create(link, room, userId, source));
			return true;
		} catch (DataIntegrityViolationException ex) {
			if (linkAnalysisRequestRepository.findByRoomAndLinkId(room, link.getId()).isPresent()) {
				return false;
			}
			throw new BusinessException(ErrorCode.E409_CONFLICT, "이미 이 방에서 분석 요청한 링크입니다.", ex);
		} catch (DataAccessException ex) {
			log.error("링크 분석 요청 이력 저장에 실패했습니다. roomId={}, linkId={}", room.getPublicId(), link.getId(), ex);
			throw new BusinessException(ErrorCode.E500_INTERNAL, "링크 분석 요청 이력 저장에 실패했습니다.", ex);
		}
	}

	private void publishProcessingRequestedEventIfNeeded(
			boolean shouldPublish,
			Link link,
			String normalizedUrl,
			String roomId,
			String source
	) {
		if (!shouldPublish) {
			return;
		}
		eventPublisher.publishEvent(new LinkProcessingRequestedEvent(link.getId(), normalizedUrl, roomId, source));
	}

	private boolean recoverDispatchFailedForManualRetry(Link link) {
		if (link.getDispatchStatus() != ProcessingDispatchStatus.DISPATCH_FAILED
				|| link.getStatus() != LinkAnalysisStatus.DISPATCH_FAILED
				|| link.getProcessingJobId() != null) {
			return false;
		}

		int updated = linkRepository.recoverDispatchFailedForManualRetry(
				link.getId(),
				ProcessingDispatchStatus.DISPATCH_FAILED,
				LinkAnalysisStatus.DISPATCH_FAILED,
				ProcessingDispatchStatus.PENDING,
				LinkAnalysisStatus.REQUESTED,
				Instant.now()
		);
		if (updated == 1) {
			log.info("DISPATCH_FAILED 링크를 수동 재시도 상태로 복구했습니다. linkId={}", link.getId());
			return true;
		}
		log.info("DISPATCH_FAILED 링크 복구가 동시 변경으로 건너뛰어졌습니다. linkId={}", link.getId());
		return false;
	}

	private record AnalysisTarget(Link link, boolean createdNewLink) {
	}

	public static final class LinkDuplicateRaceException extends RuntimeException {

		private final String normalizedUrl;

		LinkDuplicateRaceException(String normalizedUrl, Throwable cause) {
			super("Normalized URL duplicate race detected: " + normalizedUrl, cause);
			this.normalizedUrl = normalizedUrl;
		}

		public String normalizedUrl() {
			return normalizedUrl;
		}
	}
}
