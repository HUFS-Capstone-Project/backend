package com.hufs.capstone.backend.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecureRefreshTokenSecurityAdapterTest {

	private SecureRefreshTokenSecurityAdapter adapter;

	@BeforeEach
	void setUp() {
		AuthProperties properties = new AuthProperties();
		properties.getRefresh().setTokenBytes(32);
		properties.getRefresh().setHashSecret("key");
		adapter = new SecureRefreshTokenSecurityAdapter(properties);
	}

	@Test
	void shouldGenerateDistinctUrlSafeTokensWithConfiguredEntropy() {
		String first = adapter.generateRawToken();
		String second = adapter.generateRawToken();

		assertThat(first).isNotEqualTo(second);
		assertThat(first).matches("[A-Za-z0-9_-]+");
		assertThat(Base64.getUrlDecoder().decode(first)).hasSize(32);
	}

	@Test
	void shouldHashUsingHmacSha256() {
		assertThat(adapter.hash("The quick brown fox jumps over the lazy dog"))
				.isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
	}

	@Test
	void shouldRejectBlankRawToken() {
		assertThatIllegalArgumentException().isThrownBy(() -> adapter.hash(null));
		assertThatIllegalArgumentException().isThrownBy(() -> adapter.hash(" "));
	}
}
