package com.hufs.capstone.backend.auth.api.response;

public record AuthTokenBootstrapResponse(
		TokenResponse token,
		MeResponse me
) {
}
