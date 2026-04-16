package com.hufs.capstone.backend.room.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import com.hufs.capstone.backend.room.domain.repository.RoomMemberRepository;
import com.hufs.capstone.backend.room.domain.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomAccessService {

	private final RoomRepository roomRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final RoomLinkRepository roomLinkRepository;

	public Room getRoomOrThrow(String roomPublicId) {
		return roomRepository.findByPublicId(roomPublicId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "방을 찾을 수 없습니다."));
	}

	public RoomMember getMembershipOrThrow(Room room, Long userId) {
		return roomMemberRepository.findByRoomAndUserId(room, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E403_FORBIDDEN, "방 접근 권한이 없습니다."));
	}

	public Room requireMemberRoom(String roomPublicId, Long userId) {
		Room room = getRoomOrThrow(roomPublicId);
		getMembershipOrThrow(room, userId);
		return room;
	}

	public void assertLinkReadable(Long linkId, Long userId) {
		boolean accessible = roomLinkRepository.existsAccessibleLinkForUser(linkId, userId);
		if (!accessible) {
			throw new BusinessException(ErrorCode.E403_FORBIDDEN, "링크 접근 권한이 없습니다.");
		}
	}
}
