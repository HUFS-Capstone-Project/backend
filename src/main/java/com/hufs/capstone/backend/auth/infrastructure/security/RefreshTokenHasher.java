package com.hufs.capstone.backend.auth.infrastructure.security;

import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenHasher {

	private static final String ALGORITHM = "HmacSHA256";

	private final AuthProperties authProperties;

	public String hash(String rawToken) {
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			SecretKeySpec key = new SecretKeySpec(
					authProperties.getRefresh().getHashSecret().getBytes(StandardCharsets.UTF_8),
					ALGORITHM
			);
			mac.init(key);
			return HexFormat.of().formatHex(mac.doFinal(rawToken.getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Failed to hash refresh token.", ex);
		}
	}
}



