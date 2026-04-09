package com.hufs.capstone.backend.auth.api.response;

import java.time.Instant;

public record TokenResponse(
		String accessToken,
		Instant accessTokenExpiresAt,
		String refreshToken,
		Instant refreshTokenExpiresAt
) {
	public static TokenResponse web(String accessToken, Instant accessTokenExpiresAt) {
		return new TokenResponse(accessToken, accessTokenExpiresAt, null, null);
	}
}
