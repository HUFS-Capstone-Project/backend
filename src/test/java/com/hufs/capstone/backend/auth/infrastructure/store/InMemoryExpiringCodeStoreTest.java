package com.hufs.capstone.backend.auth.infrastructure.store;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.time.Duration;

class InMemoryExpiringCodeStoreTest {

	@Test
	void consumeShouldBeIdempotentWithinReplayWindow() {
		InMemoryExpiringCodeStore<String> store = new InMemoryExpiringCodeStore<>(
				Duration.ofSeconds(3),
				Duration.ofSeconds(2)
		);
		String code = store.issue("payload");

		String first = store.consume(code);
		String second = store.consume(code);

		assertThat(first).isEqualTo("payload");
		assertThat(second).isEqualTo("payload");
	}

	@Test
	void consumeShouldReturnNullAfterReplayWindow() throws Exception {
		InMemoryExpiringCodeStore<String> store = new InMemoryExpiringCodeStore<>(
				Duration.ofSeconds(2),
				Duration.ofMillis(200)
		);
		String code = store.issue("payload");

		assertThat(store.consume(code)).isEqualTo("payload");
		Thread.sleep(300);

		assertThat(store.consume(code)).isNull();
	}
}

