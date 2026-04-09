package com.hufs.capstone.backend.auth.infrastructure.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hufs.capstone.backend.auth.application.dto.TokenPair;
import com.hufs.capstone.backend.auth.domain.enums.ClientPlatform;
import com.hufs.capstone.backend.auth.domain.enums.DeviceType;
import com.hufs.capstone.backend.auth.domain.vo.ClientContext;
import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RefreshRotationReplayStoreTest {

	@SuppressWarnings("unchecked")
	private RefreshRotationReplayStore createStore(Map<String, String> redisBackingStore) {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenAnswer(invocation -> redisBackingStore.get(invocation.getArgument(0)));
		doAnswer(invocation -> {
			redisBackingStore.put(invocation.getArgument(0), invocation.getArgument(1));
			return null;
		}).when(valueOperations).set(anyString(), anyString(), any(Duration.class));

		AuthProperties authProperties = new AuthProperties();
		authProperties.getRedis().setKeyPrefix("auth");
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		return new RefreshRotationReplayStore(redisTemplate, objectMapper, authProperties);
	}

	@Test
	void shouldReturnReplayTokenPairForSameContext() {
		Map<String, String> redisBackingStore = new ConcurrentHashMap<>();
		RefreshRotationReplayStore store = createStore(redisBackingStore);
		ClientContext context = new ClientContext(DeviceType.WEB, ClientPlatform.REACT_WEB, "ua", "127.0.0.1");
		TokenPair tokenPair = new TokenPair("access", Instant.now().plusSeconds(60), "refresh", Instant.now().plusSeconds(600));

		store.save("old-hash", context, tokenPair, Duration.ofSeconds(5));

		TokenPair replay = store.findReplay("old-hash", context);
		assertThat(replay).isEqualTo(tokenPair);
	}

	@Test
	void shouldNotReturnReplayTokenPairForDifferentContext() {
		Map<String, String> redisBackingStore = new ConcurrentHashMap<>();
		RefreshRotationReplayStore store = createStore(redisBackingStore);
		ClientContext context = new ClientContext(DeviceType.WEB, ClientPlatform.REACT_WEB, "ua", "127.0.0.1");
		ClientContext differentContext = new ClientContext(DeviceType.ANDROID, ClientPlatform.CAPACITOR_ANDROID, "ua2", "10.0.0.2");
		TokenPair tokenPair = new TokenPair("access", Instant.now().plusSeconds(60), "refresh", Instant.now().plusSeconds(600));

		store.save("old-hash", context, tokenPair, Duration.ofSeconds(5));

		TokenPair replay = store.findReplay("old-hash", differentContext);
		assertThat(replay).isNull();
	}
}
