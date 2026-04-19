package com.hufs.capstone.backend.auth.oauth.userinfo;

import com.hufs.capstone.backend.auth.oauth.SocialIdentity;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import java.util.Map;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
public class OAuth2UserInfoFactory {

	public SocialIdentity from(String registrationId, OidcUser oidcUser) {
		OAuth2UserInfo userInfo = switch (registrationId.toLowerCase()) {
			case "google" -> new GoogleOidcUserInfo(oidcUser.getClaims());
			default -> throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "지원하지 않는 제공자: " + registrationId);
		};
		return userInfo.toIdentity();
	}

	public SocialIdentity from(String registrationId, Map<String, Object> attributes) {
		OAuth2UserInfo userInfo = switch (registrationId.toLowerCase()) {
			case "google" -> new GoogleOidcUserInfo(attributes);
			default -> throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "지원하지 않는 제공자: " + registrationId);
		};
		return userInfo.toIdentity();
	}
}




