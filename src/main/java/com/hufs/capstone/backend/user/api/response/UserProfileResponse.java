package com.hufs.capstone.backend.user.api.response;

import com.hufs.capstone.backend.user.application.dto.UserProfileResult;
import com.hufs.capstone.backend.user.domain.entity.User;

public record UserProfileResponse(
		Long id,
		String email,
		String nickname,
		String profileImageUrl,
		String role,
		String status,
		boolean onboardingCompleted
) {

	public static UserProfileResponse from(User user) {
		return from(UserProfileResult.from(user));
	}

	public static UserProfileResponse from(UserProfileResult result) {
		return new UserProfileResponse(
				result.id(),
				result.email(),
				result.nickname(),
				result.profileImageUrl(),
				result.role(),
				result.status(),
				result.onboardingCompleted()
		);
	}
}
