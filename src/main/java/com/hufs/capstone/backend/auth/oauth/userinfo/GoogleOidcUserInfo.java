package com.hufs.capstone.backend.auth.oauth.userinfo;

import com.hufs.capstone.backend.auth.oauth.SocialIdentity;
import com.hufs.capstone.backend.user.domain.enums.SocialProvider;
import java.util.Map;

public class GoogleOidcUserInfo implements OAuth2UserInfo {

	private final Map<String, Object> attributes;

	public GoogleOidcUserInfo(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	@Override
	public SocialIdentity toIdentity() {
		String sub = getString("sub");
		String email = getString("email");
		boolean emailVerified = Boolean.TRUE.equals(attributes.get("email_verified"));
		String nickname = getString("name");
		return new SocialIdentity(SocialProvider.GOOGLE, sub, email, emailVerified, nickname);
	}

	private String getString(String key) {
		Object value = attributes.get(key);
		if (value == null) {
			return null;
		}
		return String.valueOf(value);
	}
}



