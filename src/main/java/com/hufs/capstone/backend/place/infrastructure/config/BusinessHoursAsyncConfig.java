package com.hufs.capstone.backend.place.infrastructure.config;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
public class BusinessHoursAsyncConfig {

	public static final String BUSINESS_HOURS_TASK_EXECUTOR = "businessHoursTaskExecutor";

	@Bean(name = BUSINESS_HOURS_TASK_EXECUTOR)
	public Executor businessHoursTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("business-hours-");
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(100);
		executor.setRejectedExecutionHandler((task, executorService) ->
				log.warn("Business hours executor rejected an async task."));
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(10);
		executor.initialize();
		return executor;
	}
}
