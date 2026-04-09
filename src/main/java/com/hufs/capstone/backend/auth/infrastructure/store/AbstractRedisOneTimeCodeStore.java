package com.hufs.capstone.backend.auth.infrastructure.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.auth.application.ExpiringCodeStore;
import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Slf4j
public abstract class AbstractRedisOneTimeCodeStore<T> implements ExpiringCodeStore<T> {

	private static final DefaultRedisScript<String> CONSUME_SCRIPT = new DefaultRedisScript<>(
			"""
			local active = redis.call('GET', KEYS[1])
			if active then
			  redis.call('SET', KEYS[2], active, 'EX', ARGV[1])
			  redis.call('DEL', KEYS[1])
			  return active
			end
			local consumed = redis.call('GET', KEYS[2])
			if consumed then
			  return consumed
			end
			return nil
			""",
			String.class
	);

	private final SecureRandom secureRandom = new SecureRandom();
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final AuthProperties authProperties;
	private final Duration ttl;
	private final Duration replayWindow;
	private final Class<T> payloadType;
	private final String codeType;

	protected AbstractRedisOneTimeCodeStore(
			StringRedisTemplate redisTemplate,
			ObjectMapper objectMapper,
			AuthProperties authProperties,
			Duration ttl,
			Duration replayWindow,
			Class<T> payloadType,
			String codeType
	) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.authProperties = authProperties;
		this.ttl = ttl;
		this.replayWindow = replayWindow;
		this.payloadType = payloadType;
		this.codeType = codeType;
	}

	@Override
	public String issue(T value) {
		for (int i = 0; i < 3; i++) {
			String code = generateCode();
			String serialized = serialize(value);
			Boolean created = redisTemplate.opsForValue().setIfAbsent(activeKey(code), serialized, ttl);
			if (Boolean.TRUE.equals(created)) {
				return code;
			}
		}
		throw new IllegalStateException("Failed to issue one-time code after retries.");
	}

	@Override
	public T consume(String code) {
		Long replayTtlSeconds = Math.max(1L, replayWindow.toSeconds());
		String serialized = redisTemplate.execute(
				CONSUME_SCRIPT,
				java.util.List.of(activeKey(code), consumedKey(code)),
				String.valueOf(replayTtlSeconds)
		);
		if (serialized == null) {
			return null;
		}
		return deserialize(serialized);
	}

	private String serialize(T payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException ex) {
			log.error("Failed to serialize one-time code payload. type={}", codeType, ex);
			throw new IllegalStateException("Failed to serialize one-time code payload.", ex);
		}
	}

	private T deserialize(String serialized) {
		try {
			return objectMapper.readValue(serialized, payloadType);
		} catch (JsonProcessingException ex) {
			log.error("Failed to deserialize one-time code payload. type={}", codeType, ex);
			throw new IllegalStateException("Failed to deserialize one-time code payload.", ex);
		}
	}

	private String generateCode() {
		byte[] bytes = new byte[24];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String activeKey(String code) {
		return authProperties.getRedis().getKeyPrefix() + ":otc:" + codeType + ":active:" + code;
	}

	private String consumedKey(String code) {
		return authProperties.getRedis().getKeyPrefix() + ":otc:" + codeType + ":consumed:" + code;
	}
}

