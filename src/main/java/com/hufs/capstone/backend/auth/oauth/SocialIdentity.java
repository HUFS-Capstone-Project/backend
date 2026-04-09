package com.hufs.capstone.backend.auth.oauth;

import com.hufs.capstone.backend.user.domain.enums.SocialProvider;

public record SocialIdentity(
		SocialProvider provider,
		String providerUserId,
		String email,
		boolean emailVerified,
		String nickname,
		String profileImageUrl
) {
}



