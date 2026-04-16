package com.hufs.capstone.backend.auth.api.response;

import com.hufs.capstone.backend.user.api.response.UserProfileResponse;

public record DevMasterTokenResponse(
		UserProfileResponse me,
		TokenResponse token,
		boolean createdUser
) {
}
