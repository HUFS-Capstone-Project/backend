package com.hufs.capstone.backend.place.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.business-hours")
public record BusinessHoursProperties(
		@Valid Polling polling,
		@Valid DetailRefresh detailRefresh
) {

	public BusinessHoursProperties {
		if (polling == null) {
			polling = new Polling(false, Duration.ofMinutes(1), 50);
		}
		if (detailRefresh == null) {
			detailRefresh = new DetailRefresh(Duration.ofMinutes(2), Duration.ofMinutes(15), Duration.ofHours(1));
		}
	}

	public record Polling(
			boolean enabled,
			Duration interval,
			@Min(1) int batchSize
	) {

		public Polling {
			if (interval == null) {
				interval = Duration.ofMinutes(1);
			}
			if (batchSize < 1) {
				batchSize = 50;
			}
		}
	}

	public record DetailRefresh(Duration dispatchTimeout, Duration jobTimeout, Duration failureCooldown) {

		public DetailRefresh {
			if (dispatchTimeout == null || dispatchTimeout.isNegative() || dispatchTimeout.isZero()) {
				dispatchTimeout = Duration.ofMinutes(2);
			}
			if (jobTimeout == null || jobTimeout.isNegative() || jobTimeout.isZero()) {
				jobTimeout = Duration.ofMinutes(15);
			}
			if (failureCooldown == null || failureCooldown.isNegative() || failureCooldown.isZero()) {
				failureCooldown = Duration.ofHours(1);
			}
		}
	}
}
