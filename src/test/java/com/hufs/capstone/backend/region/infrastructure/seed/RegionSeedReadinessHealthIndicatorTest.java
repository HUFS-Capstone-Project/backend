package com.hufs.capstone.backend.region.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class RegionSeedReadinessHealthIndicatorTest {

	private final RegionSeedReadinessHealthIndicator readiness = new RegionSeedReadinessHealthIndicator();

	@Test
	void remainsOutOfServiceUntilSeedCompletes() {
		assertThat(readiness.health().getStatus()).isEqualTo(Status.OUT_OF_SERVICE);

		readiness.markReady();

		assertThat(readiness.health().getStatus()).isEqualTo(Status.UP);
	}

	@Test
	void reportsDownWhenSeedFails() {
		readiness.markFailed();

		assertThat(readiness.health().getStatus()).isEqualTo(Status.DOWN);
	}

	@Test
	void canReturnToSeedingForAnExplicitRerun() {
		readiness.markReady();

		readiness.markSeeding();

		assertThat(readiness.health().getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
	}
}
