package com.hufs.capstone.backend.room.application.dto;

import java.time.Instant;

public record CreateRoomResult(
		String roomId,
		String roomName,
		String inviteCode,
		boolean pinned,
		Instant createdAt
) {
}

