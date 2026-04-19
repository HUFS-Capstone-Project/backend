package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkResult;
import com.hufs.capstone.backend.link.application.event.LinkProcessingRequestedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
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
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public RegisterLinkResult registerWithinWriteTransaction(
			LinkUrlNormalizer.NormalizedUrl normalizedUrl,
			String roomId,
			Long userId,
			String source
	) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		RegistrationTarget target = findOrCreateLink(normalizedUrl);

		bindRoomLink(target.link(), room);
		saveRegisteredHistory(target.link(), room.getPublicId(), source);
		publishProcessingRequestedEventIfNeeded(target, normalizedUrl.normalizedUrl(), room.getPublicId(), source);

		return RegisterLinkResult.from(target.link());
	}

	private RegistrationTarget findOrCreateLink(LinkUrlNormalizer.NormalizedUrl normalizedUrl) {
		Link existing = linkRepository.findByNormalizedUrl(normalizedUrl.normalizedUrl()).orElse(null);
		if (existing != null) {
			return new RegistrationTarget(existing, false);
		}
		Link created = persistNewLink(normalizedUrl);
		return new RegistrationTarget(created, true);
	}

	private Link persistNewLink(LinkUrlNormalizer.NormalizedUrl normalizedUrl) {
		Link newLink = Link.registerPending(normalizedUrl.originalUrl(), normalizedUrl.normalizedUrl());
		try {
			return linkRepository.saveAndFlush(newLink);
		} catch (DataIntegrityViolationException ex) {
			throw new LinkDuplicateRaceException(normalizedUrl.normalizedUrl(), ex);
		} catch (DataAccessException ex) {
			log.error("링크 저장에 실패했습니다. normalizedUrl={}", normalizedUrl.normalizedUrl(), ex);
			throw new BusinessException(ErrorCode.E500_INTERNAL, "링크 저장에 실패했습니다.", ex);
		}
	}

	private void bindRoomLink(Link link, Room room) {
		try {
			roomLinkRepository.saveAndFlush(RoomLink.bind(room, link));
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException(ErrorCode.E409_CONFLICT, "이미 이 방에 등록된 링크입니다.");
		} catch (DataAccessException ex) {
			log.error("방-링크 매핑 저장에 실패했습니다. roomId={}, linkId={}", room.getPublicId(), link.getId(), ex);
			throw new BusinessException(ErrorCode.E500_INTERNAL, "방-링크 매핑 저장에 실패했습니다.", ex);
		}
	}

	private void saveRegisteredHistory(Link link, String roomId, String source) {
		try {
			linkProcessingHistoryRepository.saveAndFlush(LinkProcessingHistory.registered(link, roomId, source));
		} catch (DataAccessException ex) {
			log.warn("링크 등록 이력 저장에 실패했습니다. linkId={}, roomId={}", link.getId(), roomId, ex);
		}
	}

	private void publishProcessingRequestedEventIfNeeded(
			RegistrationTarget target,
			String normalizedUrl,
			String roomId,
			String source
	) {
		if (!target.createdNewLink()) {
			return;
		}
		eventPublisher.publishEvent(new LinkProcessingRequestedEvent(target.link().getId(), normalizedUrl, roomId, source));
	}

	private record RegistrationTarget(Link link, boolean createdNewLink) {
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

