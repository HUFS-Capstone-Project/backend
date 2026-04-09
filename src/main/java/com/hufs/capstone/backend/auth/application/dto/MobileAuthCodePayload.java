package com.hufs.capstone.backend.auth.application.dto;

import java.time.Instant;

public record MobileAuthCodePayload(
		Long userId,
		String codeChallenge,
		String codeChallengeMethod,
		Instant issuedAt
) {
}
