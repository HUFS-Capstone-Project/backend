package com.hufs.capstone.backend.place.application.dto;

import java.time.Instant;

public record RoomPlaceMemoResult(
		Long userId,
		String nickname,
		String profileImageUrl,
		String memo,
		Instant updatedAt
) {
}
