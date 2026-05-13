package com.hufs.capstone.backend.room.application.dto;

import java.time.Instant;

public record RoomMemberProfileResult(
		Long userId,
		String nickname,
		String profileImageUrl,
		Instant joinedAt,
		boolean me
) {
}
