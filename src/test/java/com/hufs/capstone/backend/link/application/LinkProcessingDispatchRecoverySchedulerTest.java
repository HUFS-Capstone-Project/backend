package com.hufs.capstone.backend.link.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.link.application.event.LinkProcessingRequestedEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ExtendWith(MockitoExtension.class)
class LinkProcessingDispatchRecoverySchedulerTest {

	@Mock
	private LinkProcessingDispatchRecoveryService recoveryService;

	@Mock
	private LinkProcessingDispatchService dispatchService;

	@Mock
	private ThreadPoolTaskExecutor dispatchExecutor;

	private LinkProcessingDispatchPolicy dispatchPolicy;
	private LinkProcessingDispatchRecoveryScheduler scheduler;

	@BeforeEach
	void setUp() {
		dispatchPolicy = new LinkProcessingDispatchPolicy();
		scheduler = new LinkProcessingDispatchRecoveryScheduler(
				dispatchPolicy,
				recoveryService,
				dispatchService,
				dispatchExecutor
		);
	}

	@Test
	void shouldUseOnlyHalfOfCurrentlyAvailableExecutorCapacity() {
		when(dispatchExecutor.getMaxPoolSize()).thenReturn(4);
		when(dispatchExecutor.getActiveCount()).thenReturn(4);
		when(dispatchExecutor.getQueueCapacity()).thenReturn(100);
		when(dispatchExecutor.getQueueSize()).thenReturn(90);
		LinkProcessingRequestedEvent event = event(1L);
		when(recoveryService.findRecoverableEvents(any(Instant.class), eq(5))).thenReturn(List.of(event));

		scheduler.recoverStalePendingDispatches();

		ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(dispatchExecutor).execute(taskCaptor.capture());
		verify(dispatchService, never()).dispatch(any());

		taskCaptor.getValue().run();
		verify(dispatchService).dispatch(event.dispatchAttemptId());
	}

	@Test
	void shouldDeferRecoveryWhenExecutorHasNoCapacity() {
		when(dispatchExecutor.getMaxPoolSize()).thenReturn(4);
		when(dispatchExecutor.getActiveCount()).thenReturn(4);
		when(dispatchExecutor.getQueueCapacity()).thenReturn(100);
		when(dispatchExecutor.getQueueSize()).thenReturn(100);

		scheduler.recoverStalePendingDispatches();

		verify(recoveryService, never()).findRecoverableEvents(any(Instant.class), any(Integer.class));
		verify(dispatchExecutor, never()).execute(any(Runnable.class));
	}

	private static LinkProcessingRequestedEvent event(Long attemptId) {
		return new LinkProcessingRequestedEvent(attemptId);
	}
}
