package com.hufs.capstone.backend.auth.api.response;

public record DevMasterTokenResponse(
		MeResponse me,
		TokenResponse token,
		boolean createdUser
) {
}
