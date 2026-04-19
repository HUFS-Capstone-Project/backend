package com.hufs.capstone.backend.auth.infrastructure.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.auth.application.dto.TokenPair;
import com.hufs.capstone.backend.auth.domain.vo.ClientContext;
import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshRotationReplayStore {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final AuthProperties authProperties;

	public void save(String oldTokenHash, ClientContext context, TokenPair tokenPair, Duration ttl) {
		ReplayEntry entry = new ReplayEntry(tokenPair, fingerprint(context));
		try {
			String serialized = objectMapper.writeValueAsString(entry);
			redisTemplate.opsForValue().set(replayKey(oldTokenHash), serialized, ttl);
		} catch (JsonProcessingException ex) {
			log.error("리프레시 재사용 응답 직렬화에 실패했습니다.", ex);
		} catch (RuntimeException ex) {
			log.warn("리프레시 재사용 응답 저장소 Redis 쓰기에 실패했습니다.", ex);
		}
	}

	public TokenPair findReplay(String oldTokenHash, ClientContext context) {
		try {
			String serialized = redisTemplate.opsForValue().get(replayKey(oldTokenHash));
			if (serialized == null) {
				return null;
			}
			ReplayEntry entry = objectMapper.readValue(serialized, ReplayEntry.class);
			if (!entry.contextFingerprint().equals(fingerprint(context))) {
				return null;
			}
			return entry.tokenPair();
		} catch (JsonProcessingException ex) {
			log.error("리프레시 재사용 응답 역직렬화에 실패했습니다.", ex);
			return null;
		} catch (RuntimeException ex) {
			log.warn("리프레시 재사용 응답 저장소 Redis 읽기에 실패했습니다.", ex);
			return null;
		}
	}

	private String replayKey(String oldTokenHash) {
		return authProperties.getRedis().getKeyPrefix() + ":refresh:replay:" + oldTokenHash;
	}

	private String fingerprint(ClientContext context) {
		String userAgent = context.userAgent() == null ? "" : context.userAgent();
		String ipAddress = context.ipAddress() == null ? "" : context.ipAddress();
		return context.deviceType() + "|" + context.clientPlatform() + "|" + userAgent + "|" + ipAddress;
	}

	private record ReplayEntry(TokenPair tokenPair, String contextFingerprint) {
	}
}


