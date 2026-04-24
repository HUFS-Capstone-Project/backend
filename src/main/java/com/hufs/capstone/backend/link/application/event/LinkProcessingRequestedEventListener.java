package com.hufs.capstone.backend.link.application.event;

import com.hufs.capstone.backend.link.application.LinkProcessingDispatchService;
import com.hufs.capstone.backend.link.infrastructure.config.LinkProcessingAsyncConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LinkProcessingRequestedEventListener {

	private final LinkProcessingDispatchService linkProcessingDispatchService;

	@Async(LinkProcessingAsyncConfig.LINK_PROCESSING_DISPATCH_TASK_EXECUTOR)
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onProcessingRequested(LinkProcessingRequestedEvent event) {
		// 트랜잭션 커밋 이후 별도 스레드에서 외부 processing 디스패치를 수행한다.
		linkProcessingDispatchService.dispatch(event);
	}
}
