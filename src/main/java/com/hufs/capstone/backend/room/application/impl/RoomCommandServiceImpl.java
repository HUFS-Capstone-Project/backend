package com.hufs.capstone.backend.room.application.impl;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.global.exception.FieldValidationException;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.application.RoomCommandService;
import com.hufs.capstone.backend.room.application.RoomInviteCodeGenerator;
import com.hufs.capstone.backend.room.application.RoomJoinRateLimiter;
import com.hufs.capstone.backend.room.application.dto.CreateRoomResult;
import com.hufs.capstone.backend.room.application.dto.JoinRoomResult;
import com.hufs.capstone.backend.room.application.event.RoomDeletedEvent;
import com.hufs.capstone.backend.room.domain.RoomNamePolicy;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import com.hufs.capstone.backend.room.domain.repository.RoomMemberRepository;
import com.hufs.capstone.backend.room.domain.repository.RoomRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomCommandServiceImpl implements RoomCommandService {

	private static final int INVITE_CODE_GENERATION_MAX_RETRY = 5;
	private static final long MAX_ROOM_MEMBERS = 6L;

	private final RoomRepository roomRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final RoomInviteCodeGenerator inviteCodeGenerator;
	private final RoomJoinRateLimiter roomJoinRateLimiter;
	private final RoomAccessService roomAccessService;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional
	public CreateRoomResult createRoom(Long userId, String roomName) {
		String normalizedRoomName = RoomNamePolicy.normalizeAndValidate(roomName);
		Room room = persistRoomWithUniqueInviteCode(userId, normalizedRoomName);
		roomMemberRepository.saveAndFlush(RoomMember.join(room, userId));
		return new CreateRoomResult(
				room.getPublicId(),
				room.getName(),
				room.getInviteCode(),
				room.getAvatarSeed(),
				false,
				room.getCreatedAt()
		);
	}

	@Override
	@Transactional
	public JoinRoomResult joinByInviteCode(Long userId, String inviteCode) {
		if (!roomJoinRateLimiter.allow(userId)) {
			throw new BusinessException(ErrorCode.E429_TOO_MANY_REQUESTS);
		}

		String normalizedInviteCode = requireInviteCode(inviteCode);
		Room room = roomRepository.findByInviteCodeForUpdate(normalizedInviteCode)
				.orElseThrow(() -> new FieldValidationException("inviteCode", "유효하지 않은 초대코드입니다.", inviteCode));

		if (roomMemberRepository.existsByRoomAndUserId(room, userId)) {
			throw new BusinessException(ErrorCode.ROOM_ALREADY_JOINED);
		}

		long currentMemberCount = roomMemberRepository.countByRoomId(room.getId());
		if (currentMemberCount >= MAX_ROOM_MEMBERS) {
			throw new BusinessException(ErrorCode.ROOM_MEMBER_LIMIT_REACHED);
		}

		try {
			roomMemberRepository.saveAndFlush(RoomMember.join(room, userId));
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException(ErrorCode.ROOM_ALREADY_JOINED);
		}

		return new JoinRoomResult(
				room.getPublicId(),
				room.getName(),
				room.getAvatarSeed(),
				false,
				room.getCreatedAt()
		);
	}

	@Override
	@Transactional
	public void renameRoom(Long userId, String roomId, String roomName) {
		String normalizedRoomName = RoomNamePolicy.normalizeAndValidate(roomName);
		Room room = roomAccessService.getRoomOrThrow(roomId);
		roomAccessService.getMembershipOrThrow(room, userId);
		room.rename(normalizedRoomName);
	}

	@Override
	@Transactional
	public void updateRoomPin(Long userId, String roomId, boolean pinned) {
		Room room = roomAccessService.getRoomOrThrow(roomId);
		RoomMember membership = roomAccessService.getMembershipOrThrow(room, userId);
		membership.updatePinned(pinned);
	}

	@Override
	@Transactional
	public void leaveRoom(Long userId, String roomId) {
		// 동일한 방에 대한 나가기 요청은 방 행 잠금으로 순차 처리한다.
		Room room = roomAccessService.getRoomForUpdateOrThrow(roomId);
		RoomMember membership = roomMemberRepository.findByRoomAndUserId(room, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_MEMBER));

		roomMemberRepository.delete(membership);

		long remainingMemberCount = roomMemberRepository.countByRoomId(room.getId());
		if (remainingMemberCount == 0) {
			eventPublisher.publishEvent(new RoomDeletedEvent(room.getId(), room.getPublicId()));
			roomRepository.delete(room);
		}
	}

	private Room persistRoomWithUniqueInviteCode(Long userId, String roomName) {
		for (int retry = 0; retry < INVITE_CODE_GENERATION_MAX_RETRY; retry++) {
			String inviteCode = inviteCodeGenerator.generate();
			if (roomRepository.existsByInviteCode(inviteCode)) {
				continue;
			}
			Room room = Room.create(UUID.randomUUID().toString(), roomName, inviteCode, userId);
			try {
				return roomRepository.saveAndFlush(room);
			} catch (DataIntegrityViolationException ignored) {
				// 새로운 초대코드로 재시도
			}
		}
		throw new BusinessException(ErrorCode.E500_INTERNAL, "초대코드 생성에 실패했습니다.");
	}

	private static String requireInviteCode(String inviteCode) {
		if (inviteCode == null || inviteCode.isBlank()) {
			throw new FieldValidationException("inviteCode", "초대코드는 필수입니다.");
		}
		return inviteCode.trim();
	}
}
