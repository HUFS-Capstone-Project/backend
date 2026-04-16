package com.hufs.capstone.backend.room.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.room.application.dto.CreateRoomResult;
import com.hufs.capstone.backend.room.application.dto.JoinRoomResult;
import com.hufs.capstone.backend.room.domain.RoomMemberRole;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import com.hufs.capstone.backend.room.domain.repository.RoomMemberRepository;
import com.hufs.capstone.backend.room.domain.repository.RoomRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomCommandService {

	private static final int INVITE_CODE_GENERATION_MAX_RETRY = 5;
	private static final long MAX_ROOM_MEMBERS = 6L;

	private final RoomRepository roomRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final RoomInviteCodeGenerator inviteCodeGenerator;
	private final RoomJoinRateLimiter roomJoinRateLimiter;

	@Transactional
	public CreateRoomResult createRoom(Long userId, String roomName) {
		String normalizedRoomName = requireRoomName(roomName);
		Room room = persistRoomWithUniqueInviteCode(userId, normalizedRoomName);
		roomMemberRepository.saveAndFlush(RoomMember.owner(room, userId));
		return new CreateRoomResult(
				room.getPublicId(),
				room.getName(),
				room.getInviteCode(),
				RoomMemberRole.OWNER,
				room.getCreatedAt()
		);
	}

	@Transactional
	public JoinRoomResult joinByInviteCode(Long userId, String inviteCode, String ipAddress) {
		if (!roomJoinRateLimiter.allow(userId, ipAddress)) {
			throw new BusinessException(ErrorCode.E429_TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
		}

		String normalizedInviteCode = requireInviteCode(inviteCode);
		Room room = roomRepository.findByInviteCodeForUpdate(normalizedInviteCode)
				.orElseThrow(() -> new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "유효하지 않은 초대코드입니다."));

		if (roomMemberRepository.existsByRoomAndUserId(room, userId)) {
			throw new BusinessException(ErrorCode.E409_CONFLICT, "이미 참여한 방입니다.");
		}

		long currentMemberCount = roomMemberRepository.countByRoomId(room.getId());
		if (currentMemberCount >= MAX_ROOM_MEMBERS) {
			throw new BusinessException(ErrorCode.E409_CONFLICT, "방 인원이 가득 찼습니다. 최대 6명까지 참여할 수 있습니다.");
		}

		try {
			roomMemberRepository.saveAndFlush(RoomMember.member(room, userId));
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException(ErrorCode.E409_CONFLICT, "이미 참여한 방입니다.");
		}

		return new JoinRoomResult(
				room.getPublicId(),
				room.getName(),
				RoomMemberRole.MEMBER,
				room.getCreatedAt()
		);
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
				// retry with a new invite code
			}
		}
		throw new BusinessException(ErrorCode.E500_INTERNAL, "초대코드 생성에 실패했습니다.");
	}

	private static String requireRoomName(String roomName) {
		if (roomName == null || roomName.isBlank()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "방 이름은 필수입니다.");
		}
		String normalized = roomName.trim();
		if (normalized.length() > 100) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "방 이름은 100자를 초과할 수 없습니다.");
		}
		return normalized;
	}

	private static String requireInviteCode(String inviteCode) {
		if (inviteCode == null || inviteCode.isBlank()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "초대코드는 필수입니다.");
		}
		return inviteCode.trim();
	}
}
