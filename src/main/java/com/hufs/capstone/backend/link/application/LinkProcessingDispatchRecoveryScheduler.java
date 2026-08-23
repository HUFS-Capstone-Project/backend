package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.application.event.LinkProcessingRequestedEvent;
import com.hufs.capstone.backend.link.infrastructure.config.LinkProcessingAsyncConfig;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LinkProcessingDispatchRecoveryScheduler {

	private final LinkProcessingDispatchPolicy dispatchPolicy;
	private final LinkProcessingDispatchRecoveryService recoveryService;
	private final LinkProcessingDispatchService dispatchService;
	private final ThreadPoolTaskExecutor dispatchExecutor;

	public LinkProcessingDispatchRecoveryScheduler(
			LinkProcessingDispatchPolicy dispatchPolicy,
			LinkProcessingDispatchRecoveryService recoveryService,
			LinkProcessingDispatchService dispatchService,
			@Qualifier(LinkProcessingAsyncConfig.LINK_PROCESSING_DISPATCH_TASK_EXECUTOR)
			ThreadPoolTaskExecutor dispatchExecutor
	) {
		this.dispatchPolicy = dispatchPolicy;
		this.recoveryService = recoveryService;
		this.dispatchService = dispatchService;
		this.dispatchExecutor = dispatchExecutor;
	}

	@Scheduled(fixedDelayString = "${app.link.dispatch.recovery-interval-ms:60000}")
	public void recoverStalePendingDispatches() {
		if (!dispatchPolicy.isRecoveryEnabled()) {
			return;
		}

		int batchSize = recoverySubmissionBudget();
		if (batchSize == 0) {
			log.debug("링크 디스패치 executor에 여유가 없어 stale 복구를 다음 주기로 미룹니다.");
			return;
		}

		List<LinkProcessingRequestedEvent> events = recoveryService.findRecoverableEvents(Instant.now(), batchSize);
		if (events.isEmpty()) {
			return;
		}

		log.info("stale 링크 디스패치 복구를 제출합니다. count={}, batchBudget={}", events.size(), batchSize);
		for (LinkProcessingRequestedEvent event : events) {
			dispatchExecutor.execute(() -> dispatchService.dispatch(event.dispatchAttemptId()));
		}
	}

	private int recoverySubmissionBudget() {
		int idleThreadCapacity = Math.max(0, dispatchExecutor.getMaxPoolSize() - dispatchExecutor.getActiveCount());
		int queueCapacity = Math.max(0, dispatchExecutor.getQueueCapacity());
		int remainingQueueCapacity = Math.max(0, queueCapacity - dispatchExecutor.getQueueSize());
		int availableCapacity = idleThreadCapacity + remainingQueueCapacity;
		if (availableCapacity == 0) {
			return 0;
		}
		int capacityWithHeadroom = Math.max(1, availableCapacity / 2);
		return Math.min(dispatchPolicy.getRecoveryBatchSize(), capacityWithHeadroom);
	}
}
