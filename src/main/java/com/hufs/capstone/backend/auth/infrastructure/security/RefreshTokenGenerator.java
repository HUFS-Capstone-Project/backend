package com.hufs.capstone.backend.auth.infrastructure.security;

import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenGenerator {

	private final SecureRandom secureRandom = new SecureRandom();
	private final AuthProperties authProperties;

	public String generate() {
		byte[] bytes = new byte[authProperties.getRefresh().getTokenBytes()];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}



