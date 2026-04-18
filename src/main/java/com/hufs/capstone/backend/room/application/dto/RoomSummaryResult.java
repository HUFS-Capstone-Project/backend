package com.hufs.capstone.backend.room.application.dto;

import java.time.Instant;

public record RoomSummaryResult(
		String roomId,
		String roomName,
		boolean pinned,
		Instant createdAt,
		long memberCount,
		long linkCount
) {
}

