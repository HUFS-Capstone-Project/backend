package com.hufs.capstone.backend.room.application.dto;

import java.time.Instant;

public record RoomDetailResult(
		String roomId,
		String roomName,
		String inviteCode,
		boolean pinned,
		long memberCount,
		long linkCount,
		Instant createdAt
) {
}

