package com.hufs.capstone.backend.link.infrastructure.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
public class LinkProcessingAsyncConfig {

	public static final String LINK_PROCESSING_DISPATCH_TASK_EXECUTOR = "linkProcessingDispatchTaskExecutor";
	public static final String REJECTED_TASKS_METRIC = "udidura.executor.rejected";

	@Bean(name = LINK_PROCESSING_DISPATCH_TASK_EXECUTOR)
	public ThreadPoolTaskExecutor linkProcessingDispatchTaskExecutor(
			LinkProcessingExecutorProperties properties,
			MeterRegistry meterRegistry
	) {
		properties.validateRelationships();
		Counter rejectedTasks = Counter.builder(REJECTED_TASKS_METRIC)
				.description("Number of tasks rejected by an application executor")
				.tag("name", LINK_PROCESSING_DISPATCH_TASK_EXECUTOR)
				.register(meterRegistry);
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("link-dispatch-");
		executor.setCorePoolSize(properties.getCorePoolSize());
		executor.setMaxPoolSize(properties.getMaxPoolSize());
		executor.setQueueCapacity(properties.getQueueCapacity());
		executor.setRejectedExecutionHandler((task, executorService) -> {
			rejectedTasks.increment();
			log.warn("링크 processing dispatch executor 큐가 가득 차 비동기 작업을 거절했습니다. stale 복구 스케줄러가 재시도합니다.");
		});
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
		executor.initialize();
		return executor;
	}
}
