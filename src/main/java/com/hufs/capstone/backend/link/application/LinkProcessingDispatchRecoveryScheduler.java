package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.application.event.LinkProcessingRequestedEvent;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkProcessingDispatchRecoveryScheduler {

	private final LinkProcessingDispatchPolicy dispatchPolicy;
	private final LinkProcessingDispatchRecoveryService recoveryService;
	private final LinkProcessingDispatchService dispatchService;

	@Scheduled(fixedDelayString = "${app.link.dispatch.recovery-interval-ms:60000}")
	public void recoverStalePendingDispatches() {
		if (!dispatchPolicy.isRecoveryEnabled()) {
			return;
		}

		List<LinkProcessingRequestedEvent> events = recoveryService.findRecoverableEvents(Instant.now());
		if (events.isEmpty()) {
			return;
		}

		log.info("stale 링크 디스패치 복구를 시작합니다. count={}", events.size());
		for (LinkProcessingRequestedEvent event : events) {
			dispatchService.dispatch(event);
		}
	}
}
