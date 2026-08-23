package com.hufs.capstone.backend.link.infrastructure.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.link.dispatch.executor")
public class LinkProcessingExecutorProperties {

	@Min(1)
	private int corePoolSize = 2;

	@Min(1)
	private int maxPoolSize = 4;

	@Min(0)
	private int queueCapacity = 100;

	@Min(0)
	private int awaitTerminationSeconds = 10;

	public void validateRelationships() {
		if (corePoolSize > maxPoolSize) {
			throw new IllegalArgumentException(
					"app.link.dispatch.executor.core-pool-size must not exceed max-pool-size"
			);
		}
	}
}
