package com.hufs.capstone.backend.auth.infrastructure.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.auth.application.ExpiringCodeStore;
import com.hufs.capstone.backend.auth.application.dto.MobileAuthCodePayload;
import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Qualifier("mobileAuthCodeStore")
public class RedisMobileAuthCodeStore extends AbstractRedisOneTimeCodeStore<MobileAuthCodePayload>
		implements ExpiringCodeStore<MobileAuthCodePayload> {

	public RedisMobileAuthCodeStore(
			StringRedisTemplate redisTemplate,
			ObjectMapper objectMapper,
			AuthProperties authProperties
	) {
		super(
				redisTemplate,
				objectMapper,
				authProperties,
				authProperties.getOneTimeCode().getMobileCodeTtl(),
				authProperties.getOneTimeCode().getReplayWindow(),
				MobileAuthCodePayload.class,
				"mobile-code"
		);
	}
}

