package com.hufs.capstone.backend.auth.domain.vo;

public record OAuthLoginContext(
		AuthClientType clientType,
		String returnPath,
		String codeChallenge,
		String codeChallengeMethod
) {
	public static OAuthLoginContext webDefault() {
		return new OAuthLoginContext(AuthClientType.WEB, "/auth/callback", null, null);
	}
}

