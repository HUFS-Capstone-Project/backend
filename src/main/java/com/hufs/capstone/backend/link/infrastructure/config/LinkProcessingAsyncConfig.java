package com.hufs.capstone.backend.link.infrastructure.config;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
public class LinkProcessingAsyncConfig {

	public static final String LINK_PROCESSING_DISPATCH_TASK_EXECUTOR = "linkProcessingDispatchTaskExecutor";

	@Bean(name = LINK_PROCESSING_DISPATCH_TASK_EXECUTOR)
	public Executor linkProcessingDispatchTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("link-dispatch-");
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(100);
		executor.setRejectedExecutionHandler((task, executorService) ->
				log.warn("링크 processing dispatch executor 큐가 가득 차 비동기 작업을 거절했습니다. stale 복구 스케줄러가 재시도합니다."));
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(10);
		executor.initialize();
		return executor;
	}
}
