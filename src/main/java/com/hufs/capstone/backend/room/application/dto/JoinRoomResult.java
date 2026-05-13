package com.hufs.capstone.backend.room.application.dto;

import java.time.Instant;

public record JoinRoomResult(
		String roomId,
		String roomName,
		String avatarSeed,
		boolean pinned,
		Instant createdAt
) {
}

