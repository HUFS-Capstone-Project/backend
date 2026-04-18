package com.hufs.capstone.backend.room.api.response;

import com.hufs.capstone.backend.room.application.dto.RoomDetailResult;
import java.time.Instant;

public record RoomDetailResponse(
		String roomId,
		String roomName,
		String inviteCode,
		boolean pinned,
		long memberCount,
		long linkCount,
		Instant createdAt
) {

	public static RoomDetailResponse from(RoomDetailResult result) {
		return new RoomDetailResponse(
				result.roomId(),
				result.roomName(),
				result.inviteCode(),
				result.pinned(),
				result.memberCount(),
				result.linkCount(),
				result.createdAt()
		);
	}
}

