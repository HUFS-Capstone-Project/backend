package com.hufs.capstone.backend.user.api.response;

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
		return new UserProfileResponse(
				user.getId(),
				user.getEmail(),
				user.getNickname(),
				user.getProfileImageUrl(),
				user.getRole().name(),
				user.getStatus().name(),
				user.isOnboardingCompleted()
		);
	}
}
