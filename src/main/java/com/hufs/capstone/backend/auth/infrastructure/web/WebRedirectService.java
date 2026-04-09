package com.hufs.capstone.backend.auth.infrastructure.web;

import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class WebRedirectService {

	private final AuthProperties authProperties;

	public String success(String returnPath, String ticket) {
		return UriComponentsBuilder.fromUriString(authProperties.getRedirect().getWebBaseUrl())
				.path(returnPath)
				.queryParam("ticket", ticket)
				.build(true)
				.toUriString();
	}

	public String failure(String returnPath, String code) {
		return UriComponentsBuilder.fromUriString(authProperties.getRedirect().getWebBaseUrl())
				.path(returnPath)
				.queryParam("error", code)
				.build(true)
				.toUriString();
	}
}



