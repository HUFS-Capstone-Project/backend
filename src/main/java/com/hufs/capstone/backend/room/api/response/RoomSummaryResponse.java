package com.hufs.capstone.backend.room.api.response;

import com.hufs.capstone.backend.room.application.dto.RoomSummaryResult;
import com.hufs.capstone.backend.room.domain.RoomMemberRole;
import java.time.Instant;

public record RoomSummaryResponse(
		String roomId,
		String roomName,
		RoomMemberRole role,
		Instant createdAt,
		long linkCount
) {

	public static RoomSummaryResponse from(RoomSummaryResult result) {
		return new RoomSummaryResponse(
				result.roomId(),
				result.roomName(),
				result.role(),
				result.createdAt(),
				result.linkCount()
		);
	}
}

