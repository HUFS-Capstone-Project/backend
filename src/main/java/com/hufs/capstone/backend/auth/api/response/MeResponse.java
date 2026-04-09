package com.hufs.capstone.backend.auth.api.response;

import com.hufs.capstone.backend.user.domain.entity.User;

public record MeResponse(
		Long id,
		String email,
		String nickname,
		String role,
		String status
) {
	public static MeResponse from(User user) {
		return new MeResponse(
				user.getId(),
				user.getEmail(),
				user.getNickname(),
				user.getRole().name(),
				user.getStatus().name()
		);
	}
}
