package com.hufs.capstone.backend.auth.oauth;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class CustomOidcUserService extends OidcUserService {

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) {
		return super.loadUser(userRequest);
	}
}



