package com.hufs.capstone.backend.auth.infrastructure.store;

import com.hufs.capstone.backend.auth.application.ExpiringCodeStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryExpiringCodeStore<T> implements ExpiringCodeStore<T> {

	private final SecureRandom secureRandom = new SecureRandom();
	private final Map<String, Entry<T>> activeStore = new ConcurrentHashMap<>();
	private final Map<String, Entry<T>> consumedStore = new ConcurrentHashMap<>();
	private final Duration ttl;
	private final Duration replayWindow;

	public InMemoryExpiringCodeStore(Duration ttl, Duration replayWindow) {
		this.ttl = ttl;
		this.replayWindow = replayWindow;
	}

	@Override
	public String issue(T value) {
		String code = generateCode();
		activeStore.put(code, new Entry<>(value, Instant.now().plus(ttl)));
		return code;
	}

	@Override
	public T consume(String code) {
		evictExpired();
		Entry<T> active = activeStore.remove(code);
		if (active != null) {
			if (Instant.now().isAfter(active.expiresAt())) {
				return null;
			}
			consumedStore.put(code, new Entry<>(active.value(), Instant.now().plus(replayWindow)));
			return active.value();
		}
		Entry<T> consumed = consumedStore.get(code);
		if (consumed == null || Instant.now().isAfter(consumed.expiresAt())) {
			return null;
		}
		return consumed.value();
	}

	private String generateCode() {
		byte[] bytes = new byte[24];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private void evictExpired() {
		Instant now = Instant.now();
		activeStore.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
		consumedStore.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
	}

	private record Entry<T>(T value, Instant expiresAt) {
	}
}



