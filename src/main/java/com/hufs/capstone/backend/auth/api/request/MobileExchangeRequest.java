package com.hufs.capstone.backend.auth.api.request;

import jakarta.validation.constraints.NotBlank;

public record MobileExchangeRequest(
		@NotBlank String code,
		@NotBlank String codeVerifier
) {
}
