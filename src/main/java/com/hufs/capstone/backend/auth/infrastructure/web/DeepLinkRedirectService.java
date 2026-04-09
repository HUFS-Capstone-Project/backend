package com.hufs.capstone.backend.auth.infrastructure.web;

import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class DeepLinkRedirectService {

	private final AuthProperties authProperties;

	public String success(String code) {
		return UriComponentsBuilder.fromUriString(authProperties.getRedirect().getAppLinkBaseUrl())
				.queryParam("code", code)
				.build(true)
				.toUriString();
	}

	public String failure(String reason) {
		return UriComponentsBuilder.fromUriString(authProperties.getRedirect().getAppLinkBaseUrl())
				.queryParam("error", reason)
				.build(true)
				.toUriString();
	}
}



