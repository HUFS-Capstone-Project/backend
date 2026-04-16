package com.hufs.capstone.backend.auth.api.response;

import com.hufs.capstone.backend.user.api.response.UserProfileResponse;

public record AuthTokenBootstrapResponse(
		TokenResponse token,
		UserProfileResponse me
) {
}
