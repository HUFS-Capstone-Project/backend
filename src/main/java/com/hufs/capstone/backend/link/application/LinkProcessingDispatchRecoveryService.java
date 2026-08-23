package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.application.event.LinkProcessingRequestedEvent;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.LinkProcessingDispatchAttemptStatus;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
import com.hufs.capstone.backend.link.domain.repository.LinkProcessingDispatchAttemptRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LinkProcessingDispatchRecoveryService {

	private final LinkProcessingDispatchAttemptRepository dispatchAttemptRepository;
	private final LinkProcessingDispatchPolicy dispatchPolicy;

	@Transactional(readOnly = true)
	public List<LinkProcessingRequestedEvent> findRecoverableEvents(Instant now) {
		return findRecoverableEvents(now, dispatchPolicy.getRecoveryBatchSize());
	}

	@Transactional(readOnly = true)
	public List<LinkProcessingRequestedEvent> findRecoverableEvents(Instant now, int batchSize) {
		if (batchSize < 1) {
			return List.of();
		}
		Instant staleBefore = now.minus(dispatchPolicy.getStaleThreshold());
		return dispatchAttemptRepository.findStaleTargets(
				LinkProcessingDispatchAttemptStatus.PENDING,
				LinkProcessingDispatchAttemptStatus.DISPATCHING,
				List.of(ProcessingDispatchStatus.PENDING, ProcessingDispatchStatus.DISPATCHING),
				LinkAnalysisStatus.REQUESTED,
				staleBefore,
				PageRequest.of(0, batchSize)
		)
				.stream()
				.map(attempt -> new LinkProcessingRequestedEvent(attempt.getId()))
				.toList();
	}
}
