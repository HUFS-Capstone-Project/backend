package com.hufs.capstone.backend.room.api.response;

import com.hufs.capstone.backend.room.application.dto.CreateRoomResult;
import com.hufs.capstone.backend.room.domain.RoomMemberRole;
import java.time.Instant;

public record CreateRoomResponse(
		String roomId,
		String roomName,
		String inviteCode,
		RoomMemberRole role,
		Instant createdAt
) {

	public static CreateRoomResponse from(CreateRoomResult result) {
		return new CreateRoomResponse(
				result.roomId(),
				result.roomName(),
				result.inviteCode(),
				result.role(),
				result.createdAt()
		);
	}
}

