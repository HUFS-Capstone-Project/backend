package com.hufs.capstone.backend.room.application.dto;

import java.time.Instant;

public record JoinRoomResult(
		String roomId,
		String roomName,
		boolean pinned,
		Instant createdAt
) {
}

