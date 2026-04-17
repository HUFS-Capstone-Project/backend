package com.hufs.capstone.backend.room.application.dto;

import com.hufs.capstone.backend.room.domain.RoomMemberRole;
import java.time.Instant;

public record RoomSummaryResult(
		String roomId,
		String roomName,
		RoomMemberRole role,
		Instant createdAt,
		long memberCount,
		long linkCount
) {
}

