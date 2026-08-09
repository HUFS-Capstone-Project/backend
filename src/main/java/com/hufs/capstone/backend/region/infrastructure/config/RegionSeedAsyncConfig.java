package com.hufs.capstone.backend.region.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RegionSeedAsyncConfig {

	public static final String REGION_SEED_TASK_EXECUTOR = "regionSeedTaskExecutor";

	@Bean(name = REGION_SEED_TASK_EXECUTOR)
	public ThreadPoolTaskExecutor regionSeedTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("region-seed-");
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(0);
		executor.setWaitForTasksToCompleteOnShutdown(false);
		executor.setAwaitTerminationSeconds(5);
		executor.setStrictEarlyShutdown(true);
		executor.initialize();
		return executor;
	}
}
