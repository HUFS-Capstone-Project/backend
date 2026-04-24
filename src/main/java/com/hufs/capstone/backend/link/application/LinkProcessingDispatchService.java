package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.external.processing.ProcessingClient;
import com.hufs.capstone.backend.external.processing.dto.CreateProcessingJobResponse;
import com.hufs.capstone.backend.link.application.event.LinkProcessingRequestedEvent;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
import com.hufs.capstone.backend.link.domain.entity.LinkProcessingHistory;
import com.hufs.capstone.backend.link.domain.repository.LinkProcessingHistoryRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkProcessingDispatchService {

	private static final String DISPATCH_FAILED_ERROR_CODE = "PROCESSING_DISPATCH_FAILED";
	private static final String DISPATCH_FAILED_ERROR_MESSAGE = "\uCC98\uB9AC \uB514\uC2A4\uD328\uCE58 \uC7AC\uC2DC\uB3C4\uAC00 \uBAA8\uB450 \uC18C\uC9C4\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";

	private final ProcessingClient processingClient;
	private final LinkRepository linkRepository;
	private final LinkProcessingHistoryRepository linkProcessingHistoryRepository;
	private final LinkProcessingDispatchPolicy dispatchPolicy;
	private final PlatformTransactionManager transactionManager;

	public void dispatch(LinkProcessingRequestedEvent event) {
		if (!claimDispatch(event.linkId())) {
			log.debug("이미 다른 실행자가 처리 중이어서 processing dispatch를 건너뜁니다. linkId={}", event.linkId());
			return;
		}

		RuntimeException lastException = null;
		for (int attempt = 1; attempt <= dispatchPolicy.getMaxAttempts(); attempt++) {
			try {
				CreateProcessingJobResponse createdJob = processingClient.createJob(
						event.normalizedUrl(),
						event.roomId(),
						event.source()
				);
				String createdJobId = requireCreatedJobId(createdJob, event.linkId());
				bindCreatedJobId(event.linkId(), createdJobId);
				return;
			} catch (RuntimeException ex) {
				lastException = ex;
				log.warn(
						"처리 디스패치에 실패했습니다. linkId={}, attempt={}/{}",
						event.linkId(),
						attempt,
						dispatchPolicy.getMaxAttempts(),
						ex
				);
				if (attempt < dispatchPolicy.getMaxAttempts() && !waitBackoff()) {
					break;
				}
			}
		}

		handleExhaustedRetry(event.linkId(), lastException);
	}

	private boolean claimDispatch(Long linkId) {
		Instant now = Instant.now();
		Instant staleBefore = now.minus(dispatchPolicy.getStaleThreshold());
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		Integer updated = transactionTemplate.execute(status -> linkRepository.claimDispatchForProcessing(
				linkId,
				LinkAnalysisStatus.REQUESTED,
				ProcessingDispatchStatus.PENDING,
				ProcessingDispatchStatus.DISPATCHING,
				ProcessingDispatchStatus.DISPATCHING,
				staleBefore,
				now
		));
		int updatedCount = updated == null ? 0 : updated;
		return updatedCount == 1;
	}

	private void bindCreatedJobId(Long linkId, String createdJobId) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		Integer updated = transactionTemplate.execute(status -> linkRepository.bindProcessingJobIdForPending(
				linkId,
				createdJobId,
				ProcessingDispatchStatus.DISPATCHING,
				ProcessingDispatchStatus.DISPATCHED,
				Instant.now()
		));
		int updatedCount = updated == null ? 0 : updated;
		if (updatedCount == 1) {
			log.info("처리 작업 디스패치를 완료했습니다. linkId={}, processingJobId={}", linkId, createdJobId);
			return;
		}
		log.info("링크 디스패치 상태가 동시 변경되어 처리 작업 바인딩을 건너뜁니다. linkId={}", linkId);
	}

	private void handleExhaustedRetry(Long linkId, RuntimeException lastException) {
		boolean dispatchMarkedFailed = markDispatchFailedIfDispatching(linkId);
		if (!dispatchMarkedFailed) {
			log.info("링크 디스패치 상태가 동시 변경되어 재시도 소진 처리를 건너뜁니다. linkId={}", linkId);
			return;
		}

		log.error("처리 디스패치 재시도가 모두 소진되어 DISPATCH_FAILED 상태로 전환합니다. linkId={}", linkId, lastException);
		saveDispatchFailedHistory(linkId);
	}

	private boolean markDispatchFailedIfDispatching(Long linkId) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		Integer updated = transactionTemplate.execute(status -> linkRepository.markDispatchFailedIfNoJob(
				linkId,
				ProcessingDispatchStatus.DISPATCHING,
				ProcessingDispatchStatus.DISPATCH_FAILED,
				LinkAnalysisStatus.DISPATCH_FAILED,
				DISPATCH_FAILED_ERROR_CODE,
				DISPATCH_FAILED_ERROR_MESSAGE,
				Instant.now()
		));
		int updatedCount = updated == null ? 0 : updated;
		return updatedCount == 1;
	}

	private void saveDispatchFailedHistory(Long linkId) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transactionTemplate.executeWithoutResult(status -> linkRepository.findById(linkId)
				.ifPresent(link -> linkProcessingHistoryRepository.save(LinkProcessingHistory.dispatchFailed(
						link,
						DISPATCH_FAILED_ERROR_CODE,
						DISPATCH_FAILED_ERROR_MESSAGE
				))));
	}

	private boolean waitBackoff() {
		try {
			Thread.sleep(dispatchPolicy.getRetryBackoff().toMillis());
			return true;
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private static String requireCreatedJobId(CreateProcessingJobResponse createdJob, Long linkId) {
		if (createdJob == null || createdJob.jobId() == null || createdJob.jobId().isBlank()) {
			throw new IllegalStateException("Processing job dispatch response does not contain jobId. linkId=" + linkId);
		}
		return createdJob.jobId();
	}
}
