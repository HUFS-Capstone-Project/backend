package com.hufs.capstone.backend.auth.api.request;

import jakarta.validation.constraints.NotBlank;

public record MobileExchangeRequest(
		@NotBlank(message = "모바일 인증 코드는 필수입니다.") String code,
		@NotBlank(message = "코드 검증값(verifier)은 필수입니다.") String codeVerifier
) {
}
