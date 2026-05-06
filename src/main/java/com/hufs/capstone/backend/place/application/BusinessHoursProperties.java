package com.hufs.capstone.backend.place.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.business-hours")
public record BusinessHoursProperties(
		@Valid Polling polling
) {

	public BusinessHoursProperties {
		if (polling == null) {
			polling = new Polling(false, Duration.ofMinutes(1), 50);
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
}
