package com.hufs.capstone.backend.auth.application.port;

import com.hufs.capstone.backend.user.domain.enums.UserRole;
import com.hufs.capstone.backend.user.domain.enums.UserStatus;
import java.time.Instant;

public interface AccessTokenIssuer {

	IssuedAccessToken issue(Long userId, UserRole role, UserStatus status, Instant issuedAt);

	record IssuedAccessToken(String value, Instant expiresAt) {
	}
}
