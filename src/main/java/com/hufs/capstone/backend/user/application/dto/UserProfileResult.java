package com.hufs.capstone.backend.user.application.dto;

import com.hufs.capstone.backend.user.domain.entity.User;

public record UserProfileResult(
		Long id,
		String email,
		String nickname,
		String profileImageUrl,
		String role,
		String status,
		boolean onboardingCompleted
) {

	public static UserProfileResult from(User user) {
		return new UserProfileResult(
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
