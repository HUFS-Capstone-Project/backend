package com.hufs.capstone.backend.room.application;

import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;

public interface RoomAccessService {

	Room getRoomOrThrow(String roomPublicId);

	Room getRoomForUpdateOrThrow(String roomPublicId);

	RoomMember getMembershipOrThrow(Room room, Long userId);

	Room requireMemberRoom(String roomPublicId, Long userId);
}
