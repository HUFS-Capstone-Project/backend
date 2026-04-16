package com.hufs.capstone.backend.room.api.response;

import com.hufs.capstone.backend.room.application.dto.JoinRoomResult;
import com.hufs.capstone.backend.room.domain.RoomMemberRole;
import java.time.Instant;

public record JoinRoomResponse(
		String roomId,
		String roomName,
		RoomMemberRole role,
		Instant createdAt
) {

	public static JoinRoomResponse from(JoinRoomResult result) {
		return new JoinRoomResponse(result.roomId(), result.roomName(), result.role(), result.createdAt());
	}
}

