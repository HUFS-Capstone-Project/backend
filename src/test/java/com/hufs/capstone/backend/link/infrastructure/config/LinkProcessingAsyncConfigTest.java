package com.hufs.capstone.backend.link.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class LinkProcessingAsyncConfigTest {

	@Test
	void shouldApplyConfiguredPoolSizesAndCountRejectedTasks() throws InterruptedException {
		LinkProcessingExecutorProperties properties = new LinkProcessingExecutorProperties();
		properties.setCorePoolSize(1);
		properties.setMaxPoolSize(1);
		properties.setQueueCapacity(0);
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		ThreadPoolTaskExecutor executor = new LinkProcessingAsyncConfig()
				.linkProcessingDispatchTaskExecutor(properties, meterRegistry);
		CountDownLatch running = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);

		try {
			executor.execute(() -> {
				running.countDown();
				await(release);
			});
			assertThat(running.await(1, TimeUnit.SECONDS)).isTrue();

			executor.execute(() -> { });

			assertThat(executor.getCorePoolSize()).isEqualTo(1);
			assertThat(executor.getMaxPoolSize()).isEqualTo(1);
			assertThat(meterRegistry.get(LinkProcessingAsyncConfig.REJECTED_TASKS_METRIC)
					.tag("name", LinkProcessingAsyncConfig.LINK_PROCESSING_DISPATCH_TASK_EXECUTOR)
					.counter()
					.count()).isEqualTo(1.0);
		} finally {
			release.countDown();
			executor.shutdown();
			meterRegistry.close();
		}
	}

	@Test
	void shouldRejectCorePoolSizeGreaterThanMaxPoolSize() {
		LinkProcessingExecutorProperties properties = new LinkProcessingExecutorProperties();
		properties.setCorePoolSize(5);
		properties.setMaxPoolSize(4);
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

		try {
			assertThatThrownBy(() -> new LinkProcessingAsyncConfig()
					.linkProcessingDispatchTaskExecutor(properties, meterRegistry))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("core-pool-size");
		} finally {
			meterRegistry.close();
		}
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}
