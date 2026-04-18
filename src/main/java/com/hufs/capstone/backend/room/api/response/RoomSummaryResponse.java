package com.hufs.capstone.backend.room.api.response;

import com.hufs.capstone.backend.room.application.dto.RoomSummaryResult;
import java.time.Instant;

public record RoomSummaryResponse(
		String roomId,
		String roomName,
		boolean pinned,
		Instant createdAt,
		long memberCount,
		long linkCount
) {

	public static RoomSummaryResponse from(RoomSummaryResult result) {
		return new RoomSummaryResponse(
				result.roomId(),
				result.roomName(),
				result.pinned(),
				result.createdAt(),
				result.memberCount(),
				result.linkCount()
		);
	}
}

