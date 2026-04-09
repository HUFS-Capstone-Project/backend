package com.hufs.capstone.backend.auth.application.dto;

import java.time.Instant;

public record WebLoginTicketPayload(
		Long userId,
		String accessToken,
		Instant accessTokenExpiresAt,
		Instant issuedAt
) {
}
