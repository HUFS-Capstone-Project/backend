package com.hufs.capstone.backend.auth.security;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

	private SecurityUtils() {
	}

	public static Long currentUserIdOrThrow() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
			throw new BusinessException(ErrorCode.E401_UNAUTHORIZED, "인증된 사용자를 찾을 수 없습니다.");
		}
		return principal.userId();
	}
}




