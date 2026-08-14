package com.hufs.capstone.backend.region.infrastructure.seed;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class RegionSeedReadinessHealthIndicator implements HealthIndicator {

	private final AtomicReference<State> state = new AtomicReference<>(State.SEEDING);

	@Override
	public Health health() {
		return switch (state.get()) {
			case SEEDING -> Health.outOfService().withDetail("state", "seeding").build();
			case READY -> Health.up().withDetail("state", "ready").build();
			case FAILED -> Health.down().withDetail("state", "failed").build();
		};
	}

	public void markSeeding() {
		state.set(State.SEEDING);
	}

	public void markReady() {
		state.set(State.READY);
	}

	public void markFailed() {
		state.set(State.FAILED);
	}

	private enum State {
		SEEDING,
		READY,
		FAILED
	}
}
