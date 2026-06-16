package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.external.processing.ProcessingErrorCodes;
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
	private final LinkProcessingDispatchPolicy dispatchPolicy;

	@Transactional
	public LinkAnalysisRequestResult requestWithinWriteTransaction(
			LinkUrlNormalizer.NormalizedUrl normalizedUrl,
			String roomId,
			Long userId,
			String source
	) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		AnalysisTarget target = findOrCreateLink(normalizedUrl);
		boolean recoveredInstagramRateLimit = recoverExpiredInstagramRateLimitForSameUrl(
				target.link(),
				normalizedUrl.normalizedUrl()
		);
		AnalysisRequestTarget requestTarget = findOrCreateAnalysisRequest(
				target.link(),
				room,
				userId,
				source,
				normalizedUrl.originalUrl()
		);
		boolean recoveredDispatchFailed = recoverDispatchFailedForManualRetry(target.link());
		boolean staleRequestedWithoutJob = isStaleRequestedWithoutJob(target.link());

		publishProcessingRequestedEventIfNeeded(
				target.createdNewLink() || recoveredInstagramRateLimit || recoveredDispatchFailed || staleRequestedWithoutJob,
				target.link(),
				requestTarget.analysisRequest().getOriginalUrl(),
				normalizedUrl.normalizedUrl(),
				room.getPublicId(),
				source
		);

		return LinkAnalysisRequestResult.from(
				target.link(),
				requestTarget.analysisRequest().getId(),
				requestTarget.createdNewRequest()
		);
	}

	@Transactional
	public LinkAnalysisRequestResult retryWithinWriteTransaction(Long userId, String roomId, Long analysisRequestId) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		LinkAnalysisRequest analysisRequest = linkAnalysisRequestRepository
				.findWithRoomAndLinkByIdForUpdate(analysisRequestId)
				.orElseThrow(() -> new BusinessException(ErrorCode.LINK_ANALYSIS_REQUEST_NOT_FOUND));
		if (!analysisRequest.getRoom().getId().equals(room.getId())) {
			throw new BusinessException(ErrorCode.LINK_ANALYSIS_REQUEST_NOT_FOUND);
		}

		Link link = analysisRequest.getLink();
		validateRetryable(link);
		boolean needsReset = link.getStatus() == LinkAnalysisStatus.FAILED;
		if (needsReset) {
			resetFailedForRetry(link);
		} else if (link.getStatus() == LinkAnalysisStatus.DISPATCH_FAILED) {
			if (!recoverDispatchFailedForManualRetry(link)) {
				throw new BusinessException(ErrorCode.LINK_ANALYSIS_RETRY_STATE_CHANGED);
			}
		}

		publishProcessingRequestedEventIfNeeded(
				true,
				link,
				analysisRequest.getOriginalUrl(),
				link.getNormalizedUrl(),
				room.getPublicId(),
				analysisRequest.getSource()
		);

		Link reloaded = linkRepository.findById(link.getId()).orElse(link);
		return LinkAnalysisRequestResult.from(reloaded, analysisRequest.getId(), false);
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
			log.error("Failed to save link. normalizedUrl={}", normalizedUrl.normalizedUrl(), ex);
			throw new BusinessException(ErrorCode.E500_INTERNAL, "링크 저장에 실패했습니다.", ex);
		}
	}

	private AnalysisRequestTarget findOrCreateAnalysisRequest(
			Link link,
			Room room,
			Long userId,
			String source,
			String originalUrl
	) {
		LinkAnalysisRequest existing = linkAnalysisRequestRepository.findByRoomAndLinkId(room, link.getId())
				.orElse(null);
		if (existing != null) {
			return new AnalysisRequestTarget(existing, false);
		}

		try {
			LinkAnalysisRequest saved =
					linkAnalysisRequestRepository.saveAndFlush(LinkAnalysisRequest.create(link, room, userId, source, originalUrl));
			return new AnalysisRequestTarget(saved, true);
		} catch (DataIntegrityViolationException ex) {
			throw new LinkAnalysisRequestDuplicateRaceException(room.getPublicId(), link.getId(), ex);
		} catch (DataAccessException ex) {
			log.error("Failed to save link analysis request. roomId={}, linkId={}", room.getPublicId(), link.getId(), ex);
			throw new BusinessException(ErrorCode.E500_INTERNAL, "링크 분석 요청 저장에 실패했습니다.", ex);
		}
	}

	private void publishProcessingRequestedEventIfNeeded(
			boolean shouldPublish,
			Link link,
			String originalUrl,
			String normalizedUrl,
			String roomId,
			String source
	) {
		if (!shouldPublish) {
			return;
		}
		eventPublisher.publishEvent(new LinkProcessingRequestedEvent(link.getId(), originalUrl, normalizedUrl, roomId, source));
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
			log.info("Recovered DISPATCH_FAILED link for manual redispatch. linkId={}", link.getId());
			return true;
		}
		log.info("Skipped DISPATCH_FAILED recovery because the link changed concurrently. linkId={}", link.getId());
		return false;
	}

	private boolean recoverExpiredInstagramRateLimitForSameUrl(Link link, String canonicalUrl) {
		if (!isRecoverableInstagramRateLimit(link, canonicalUrl)) {
			return false;
		}
		if (isInstagramCooldownActive(link)) {
			return false;
		}
		resetFailedForRetry(link);
		log.info("Recovered expired Instagram rate-limited link for redispatch. linkId={}", link.getId());
		return true;
	}

	private void validateRetryable(Link link) {
		if (link.getStatus() == LinkAnalysisStatus.SUCCEEDED || link.getStatus() == LinkAnalysisStatus.PROCESSING) {
			throw new BusinessException(ErrorCode.LINK_ANALYSIS_RETRY_NOT_ALLOWED);
		}
		if (link.getStatus() == LinkAnalysisStatus.REQUESTED) {
			if (isStaleRequestedWithoutJob(link)) {
				return;
			}
			throw new BusinessException(ErrorCode.LINK_ANALYSIS_NOT_EXPIRED);
		}
		if (link.getStatus() == LinkAnalysisStatus.DISPATCH_FAILED) {
			if (link.getProcessingJobId() == null) {
				return;
			}
			throw new BusinessException(ErrorCode.LINK_ANALYSIS_RETRY_NOT_ALLOWED);
		}
		if (link.getStatus() == LinkAnalysisStatus.FAILED) {
			rejectInstagramCooldownIfActive(link, link.getNormalizedUrl());
			if (Boolean.TRUE.equals(link.getRetryable())) {
				return;
			}
			throw new BusinessException(ErrorCode.LINK_ANALYSIS_RETRY_NOT_ALLOWED);
		}
		throw new BusinessException(ErrorCode.LINK_ANALYSIS_RETRY_NOT_ALLOWED);
	}

	private void resetFailedForRetry(Link link) {
		int updated = linkRepository.resetForManualRetry(
				link.getId(),
				LinkAnalysisStatus.FAILED,
				ProcessingDispatchStatus.PENDING,
				LinkAnalysisStatus.REQUESTED,
				Instant.now()
		);
		if (updated != 1) {
			throw new BusinessException(ErrorCode.LINK_ANALYSIS_RETRY_STATE_CHANGED);
		}
	}

	private boolean isStaleRequestedWithoutJob(Link link) {
		if (link.getStatus() != LinkAnalysisStatus.REQUESTED || link.getProcessingJobId() != null) {
			return false;
		}
		if (link.getDispatchStatus() != ProcessingDispatchStatus.PENDING
				&& link.getDispatchStatus() != ProcessingDispatchStatus.DISPATCHING) {
			return false;
		}
		Instant updatedAt = link.getUpdatedAt();
		return updatedAt != null && updatedAt.plus(dispatchPolicy.getStaleThreshold()).isBefore(Instant.now());
	}

	private void rejectInstagramCooldownIfActive(Link link, String canonicalUrl) {
		if (!isRecoverableInstagramRateLimit(link, canonicalUrl)) {
			return;
		}
		if (isInstagramCooldownActive(link)) {
			throw new BusinessException(
					ErrorCode.LINK_ANALYSIS_INSTAGRAM_COOLDOWN
			);
		}
	}

	private boolean isRecoverableInstagramRateLimit(Link link, String canonicalUrl) {
		return LinkUrlNormalizer.isInstagramCanonicalUrl(canonicalUrl)
				&& link.getStatus() == LinkAnalysisStatus.FAILED
				&& ProcessingErrorCodes.INSTAGRAM_RATE_LIMITED.equals(link.getErrorCode())
				&& Boolean.TRUE.equals(link.getRetryable());
	}

	private boolean isInstagramCooldownActive(Link link) {
		Integer cooldownSeconds = link.getCooldownSeconds();
		Instant updatedAt = link.getUpdatedAt();
		if (cooldownSeconds == null || updatedAt == null) {
			return false;
		}
		return updatedAt.plusSeconds(cooldownSeconds).isAfter(Instant.now());
	}

	private record AnalysisTarget(Link link, boolean createdNewLink) {
	}

	private record AnalysisRequestTarget(LinkAnalysisRequest analysisRequest, boolean createdNewRequest) {
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

	public static final class LinkAnalysisRequestDuplicateRaceException extends RuntimeException {

		private final String roomId;
		private final Long linkId;

		LinkAnalysisRequestDuplicateRaceException(String roomId, Long linkId, Throwable cause) {
			super("Room link analysis request duplicate race detected: roomId=" + roomId + ", linkId=" + linkId, cause);
			this.roomId = roomId;
			this.linkId = linkId;
		}

		public String roomId() {
			return roomId;
		}

		public Long linkId() {
			return linkId;
		}
	}
}
