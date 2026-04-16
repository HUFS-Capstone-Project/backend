package com.hufs.capstone.backend.room.api.response;

import com.hufs.capstone.backend.room.application.dto.RoomDetailResult;
import com.hufs.capstone.backend.room.domain.RoomMemberRole;
import java.time.Instant;

public record RoomDetailResponse(
		String roomId,
		String roomName,
		String inviteCode,
		RoomMemberRole role,
		long memberCount,
		long linkCount,
		Instant createdAt
) {

	public static RoomDetailResponse from(RoomDetailResult result) {
		return new RoomDetailResponse(
				result.roomId(),
				result.roomName(),
				result.inviteCode(),
				result.role(),
				result.memberCount(),
				result.linkCount(),
				result.createdAt()
		);
	}
}

