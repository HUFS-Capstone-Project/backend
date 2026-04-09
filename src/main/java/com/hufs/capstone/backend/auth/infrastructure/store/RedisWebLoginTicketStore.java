package com.hufs.capstone.backend.auth.infrastructure.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.auth.application.ExpiringCodeStore;
import com.hufs.capstone.backend.auth.application.dto.WebLoginTicketPayload;
import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Qualifier("webLoginTicketStore")
public class RedisWebLoginTicketStore extends AbstractRedisOneTimeCodeStore<WebLoginTicketPayload>
		implements ExpiringCodeStore<WebLoginTicketPayload> {

	public RedisWebLoginTicketStore(
			StringRedisTemplate redisTemplate,
			ObjectMapper objectMapper,
			AuthProperties authProperties
	) {
		super(
				redisTemplate,
				objectMapper,
				authProperties,
				authProperties.getOneTimeCode().getTicketTtl(),
				authProperties.getOneTimeCode().getReplayWindow(),
				WebLoginTicketPayload.class,
				"web-ticket"
		);
	}
}

