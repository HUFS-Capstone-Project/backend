package com.hufs.capstone.backend.auth.security;

import com.hufs.capstone.backend.user.domain.enums.UserRole;
import com.hufs.capstone.backend.user.domain.enums.UserStatus;

public record AuthUserPrincipal(
		Long userId,
		UserRole role,
		UserStatus status
) {
}



