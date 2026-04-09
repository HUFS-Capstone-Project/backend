package com.hufs.capstone.backend.auth.application.dto;

import java.time.Instant;

public record TokenPair(
		String accessToken,
		Instant accessTokenExpiresAt,
		String refreshToken,
		Instant refreshTokenExpiresAt
) {
}
