package com.hufs.capstone.backend.room.application.impl;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import com.hufs.capstone.backend.room.domain.repository.RoomMemberRepository;
import com.hufs.capstone.backend.room.domain.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomAccessServiceImpl implements RoomAccessService {

	private final RoomRepository roomRepository;
	private final RoomMemberRepository roomMemberRepository;

	@Override
	public Room getRoomOrThrow(String roomPublicId) {
		return roomRepository.findByPublicId(roomPublicId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
	}

	@Override
	public Room getRoomForUpdateOrThrow(String roomPublicId) {
		return roomRepository.findByPublicIdForUpdate(roomPublicId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
	}

	@Override
	public RoomMember getMembershipOrThrow(Room room, Long userId) {
		return roomMemberRepository.findByRoomAndUserId(room, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_ACCESS_FORBIDDEN));
	}

	@Override
	public Room requireMemberRoom(String roomPublicId, Long userId) {
		Room room = getRoomOrThrow(roomPublicId);
		getMembershipOrThrow(room, userId);
		return room;
	}
}

