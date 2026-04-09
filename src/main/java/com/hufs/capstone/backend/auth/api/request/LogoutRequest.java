package com.hufs.capstone.backend.auth.api.request;

public record LogoutRequest(
		String refreshToken
) {
}
