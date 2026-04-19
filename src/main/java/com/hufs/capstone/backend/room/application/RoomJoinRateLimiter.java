package com.hufs.capstone.backend.room.application;

import com.hufs.capstone.backend.room.infrastructure.config.RoomProperties;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class RoomJoinRateLimiter {

	private static final String KEY_PREFIX = "room:join:rate:";
	private static final RedisScript<Long> INCREMENT_WITH_TTL_SCRIPT = new DefaultRedisScript<>(
			"""
					local current = redis.call('INCR', KEYS[1])
					if current == 1 then
					  redis.call('PEXPIRE', KEYS[1], ARGV[1])
					end
					return current
					""",
			Long.class
	);

	private final StringRedisTemplate redisTemplate;
	private final int maxAttempts;
	private final Duration window;

	public RoomJoinRateLimiter(StringRedisTemplate redisTemplate, RoomProperties roomProperties) {
		this.redisTemplate = redisTemplate;
		this.maxAttempts = roomProperties.getJoinRateLimit().getMaxAttempts();
		this.window = roomProperties.getJoinRateLimit().getWindow();
	}

	public boolean allow(Long userId, String ipAddress) {
		String key = KEY_PREFIX + userId + ":" + sanitizeIp(ipAddress);
		try {
			Long attempts = redisTemplate.execute(
					INCREMENT_WITH_TTL_SCRIPT,
					List.of(key),
					String.valueOf(window.toMillis())
			);
			if (attempts == null) {
				return true;
			}
			return attempts <= maxAttempts;
		} catch (DataAccessException ex) {
			log.warn("방 참여 속도 제한 예외로 차단 대신 허용 처리합니다. userId={}, ip={}", userId, ipAddress, ex);
			return true;
		}
	}

	private static String sanitizeIp(String ipAddress) {
		if (!StringUtils.hasText(ipAddress)) {
			return "unknown";
		}
		return ipAddress.trim().replace(':', '_');
	}
}

