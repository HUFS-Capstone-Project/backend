package com.hufs.capstone.backend.auth.oauth.userinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OAuth2UserInfoFactoryTest {

	private final OAuth2UserInfoFactory factory = new OAuth2UserInfoFactory();

	@Test
	void fromShouldRejectUnsupportedProviderWithDedicatedErrorCode() {
		assertThatThrownBy(() -> factory.from("naver", Map.of()))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.OAUTH_PROVIDER_UNSUPPORTED));
	}
}
