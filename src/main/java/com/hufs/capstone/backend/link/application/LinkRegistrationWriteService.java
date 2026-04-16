package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkResult;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkProcessingHistory;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.link.domain.repository.LinkProcessingHistoryRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkRegistrationWriteService {

	private final LinkRepository linkRepository;
	private final RoomLinkRepository roomLinkRepository;
	private final LinkProcessingHistoryRepository linkProcessingHistoryRepository;
	private final RoomAccessService roomAccessService;

	@Transactional
	public RegisterLinkResult registerWithinWriteTransaction(
			LinkUrlNormalizer.NormalizedUrl normalizedUrl,
			String roomId,
			Long userId,
			String source,
			String newProcessingJobId
	) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		Link link = linkRepository.findByNormalizedUrl(normalizedUrl.normalizedUrl())
				.orElseGet(() -> persistNewLink(normalizedUrl, newProcessingJobId));

		bindRoomLink(link, room);
		saveRegisteredHistory(link, room.getPublicId(), source);
		return new RegisterLinkResult(link.getId(), link.getProcessingJobId(), link.getStatus());
	}

	private Link persistNewLink(LinkUrlNormalizer.NormalizedUrl normalizedUrl, String processingJobId) {
		if (processingJobId == null || processingJobId.isBlank()) {
			throw new BusinessException(ErrorCode.E500_INTERNAL, "신규 링크 생성에는 processing jobId가 필요합니다.");
		}
		Link newLink = Link.register(normalizedUrl.originalUrl(), normalizedUrl.normalizedUrl(), processingJobId);
		try {
			return linkRepository.saveAndFlush(newLink);
		} catch (DataIntegrityViolationException ex) {
			throw new LinkDuplicateRaceException(normalizedUrl.normalizedUrl(), ex);
		} catch (DataAccessException ex) {
			log.error("Failed to persist link. normalizedUrl={}", normalizedUrl.normalizedUrl(), ex);
			throw new BusinessException(ErrorCode.E500_INTERNAL, "링크 저장에 실패했습니다.", ex);
		}
	}

	private void bindRoomLink(Link link, Room room) {
		try {
			roomLinkRepository.saveAndFlush(RoomLink.bind(room, link));
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException(ErrorCode.E409_CONFLICT, "이미 이 방에 등록된 링크입니다.");
		} catch (DataAccessException ex) {
			log.error("Failed to save room link mapping. roomId={}, linkId={}", room.getPublicId(), link.getId(), ex);
			throw new BusinessException(ErrorCode.E500_INTERNAL, "방 링크 매핑 저장에 실패했습니다.", ex);
		}
	}

	private void saveRegisteredHistory(Link link, String roomId, String source) {
		try {
			linkProcessingHistoryRepository.saveAndFlush(LinkProcessingHistory.registered(link, roomId, source));
		} catch (DataAccessException ex) {
			log.warn("Failed to save link registered history. linkId={}, roomId={}", link.getId(), roomId, ex);
		}
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
