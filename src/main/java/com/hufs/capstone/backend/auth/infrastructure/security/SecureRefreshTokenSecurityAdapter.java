package com.hufs.capstone.backend.auth.infrastructure.security;

import com.hufs.capstone.backend.auth.application.port.RefreshTokenSecurityPort;
import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecureRefreshTokenSecurityAdapter implements RefreshTokenSecurityPort {

	private static final String HASH_ALGORITHM = "HmacSHA256";

	private final SecureRandom secureRandom = new SecureRandom();
	private final AuthProperties authProperties;

	@Override
	public String generateRawToken() {
		byte[] bytes = new byte[authProperties.getRefresh().getTokenBytes()];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	@Override
	public String hash(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new IllegalArgumentException("rawToken must not be blank");
		}
		try {
			Mac mac = Mac.getInstance(HASH_ALGORITHM);
			SecretKeySpec key = new SecretKeySpec(
					authProperties.getRefresh().getHashSecret().getBytes(StandardCharsets.UTF_8),
					HASH_ALGORITHM
			);
			mac.init(key);
			return HexFormat.of().formatHex(mac.doFinal(rawToken.getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Failed to hash refresh token.", ex);
		}
	}
}
