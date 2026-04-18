package com.hufs.capstone.backend.room.api.response;

import com.hufs.capstone.backend.room.application.dto.JoinRoomResult;
import java.time.Instant;

public record JoinRoomResponse(
		String roomId,
		String roomName,
		boolean pinned,
		Instant createdAt
) {

	public static JoinRoomResponse from(JoinRoomResult result) {
		return new JoinRoomResponse(result.roomId(), result.roomName(), result.pinned(), result.createdAt());
	}
}

