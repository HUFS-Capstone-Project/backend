package com.hufs.capstone.backend.room.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.hufs.capstone.backend.room.infrastructure.config.RoomProperties;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class RoomJoinRateLimiterTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	private RoomJoinRateLimiter rateLimiter;

	@BeforeEach
	void setUp() {
		RoomProperties properties = new RoomProperties();
		properties.getJoinRateLimit().setMaxAttempts(5);
		properties.getJoinRateLimit().setWindow(Duration.ofMinutes(1));
		rateLimiter = new RoomJoinRateLimiter(redisTemplate, properties);
	}

	@Test
	void allowShouldReturnTrueWhenAttemptCountWithinLimit() {
		doReturn(3L).when(redisTemplate).execute(any(), anyList(), anyString());

		boolean allowed = rateLimiter.allow(100L, "127.0.0.1");

		assertThat(allowed).isTrue();
	}

	@Test
	void allowShouldReturnFalseWhenAttemptCountExceedsLimit() {
		doReturn(6L).when(redisTemplate).execute(any(), anyList(), anyString());

		boolean allowed = rateLimiter.allow(100L, "127.0.0.1");

		assertThat(allowed).isFalse();
	}

	@Test
	void allowShouldFailOpenWhenRedisIsUnavailable() {
		doThrow(new RedisConnectionFailureException("redis down"))
				.when(redisTemplate)
				.execute(any(), anyList(), anyString());

		boolean allowed = rateLimiter.allow(100L, "127.0.0.1");

		assertThat(allowed).isTrue();
	}
}
