package com.hufs.capstone.backend.link.application.event;

import com.hufs.capstone.backend.link.application.LinkProcessingDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LinkProcessingRequestedEventListener {

	private final LinkProcessingDispatchService linkProcessingDispatchService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onProcessingRequested(LinkProcessingRequestedEvent event) {
		// 임시 타협안으로 트랜잭션 커밋 후(AFTER_COMMIT)에 동기 디스패치를 수행한다.
		linkProcessingDispatchService.dispatch(event);
	}
}
