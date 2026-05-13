package com.hufs.capstone.backend.room.api.response;

import com.hufs.capstone.backend.room.application.dto.CreateRoomResult;
import java.time.Instant;

public record CreateRoomResponse(
		String roomId,
		String roomName,
		String inviteCode,
		String avatarSeed,
		boolean pinned,
		Instant createdAt
) {

	public static CreateRoomResponse from(CreateRoomResult result) {
		return new CreateRoomResponse(
				result.roomId(),
				result.roomName(),
				result.inviteCode(),
				result.avatarSeed(),
				result.pinned(),
				result.createdAt()
		);
	}
}

