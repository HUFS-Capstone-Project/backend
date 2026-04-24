package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.external.processing.ProcessingClient;
import com.hufs.capstone.backend.external.processing.dto.CreateProcessingJobResponse;
import com.hufs.capstone.backend.link.application.event.LinkProcessingRequestedEvent;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
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

	private final ProcessingClient processingClient;
	private final LinkRepository linkRepository;
	private final LinkProcessingDispatchPolicy dispatchPolicy;
	private final PlatformTransactionManager transactionManager;

	public void dispatch(LinkProcessingRequestedEvent event) {
		Link snapshot = linkRepository.findById(event.linkId()).orElse(null);
		if (snapshot == null) {
			log.warn("링크가 없어 처리 디스패치를 건너뜁니다. linkId={}", event.linkId());
			return;
		}
		if (!snapshot.isDispatchPending()) {
			log.debug(
					"디스패치 상태가 pending이 아니어서 처리 디스패치를 건너뜁니다. linkId={}, dispatchStatus={}, processingJobId={}",
					event.linkId(),
					snapshot.getDispatchStatus(),
					snapshot.getProcessingJobId()
			);
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
				if (attempt < dispatchPolicy.getMaxAttempts()) {
					waitBackoff();
				}
			}
		}

		handleExhaustedRetry(event.linkId(), lastException);
	}

	private void bindCreatedJobId(Long linkId, String createdJobId) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		Integer updated = transactionTemplate.execute(status -> linkRepository.bindProcessingJobIdForPending(
				linkId,
				createdJobId,
				ProcessingDispatchStatus.PENDING,
				ProcessingDispatchStatus.DISPATCHED,
				Instant.now()
		));
		int updatedCount = updated == null ? 0 : updated;
		if (updatedCount == 1) {
			log.info("처리 작업 디스패치를 완료했습니다. linkId={}, processingJobId={}", linkId, createdJobId);
			return;
		}
		log.info(
				"링크 디스패치 상태가 동시 변경되어 처리 작업 바인딩을 건너뜁니다. linkId={}",
				linkId
		);
	}

	private void handleExhaustedRetry(Long linkId, RuntimeException lastException) {
		boolean dispatchMarkedFailed = markDispatchFailedIfPending(linkId);
		if (!dispatchMarkedFailed) {
			log.info("링크 디스패치 상태가 동시 변경되어 재시도 소진 처리를 건너뜁니다. linkId={}", linkId);
			return;
		}

		log.error(
				"처리 디스패치 재시도가 모두 소진되어 DISPATCH_FAILED 상태로 전환합니다. linkId={}",
				linkId,
				lastException
		);
	}

	private boolean markDispatchFailedIfPending(Long linkId) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		Integer updated = transactionTemplate.execute(status -> linkRepository.markDispatchFailedIfNoJob(
				linkId,
				ProcessingDispatchStatus.PENDING,
				ProcessingDispatchStatus.DISPATCH_FAILED,
				LinkAnalysisStatus.DISPATCH_FAILED,
				"PROCESSING_DISPATCH_FAILED",
				"처리 디스패치 재시도가 모두 소진되었습니다.",
				Instant.now()
		));
		int updatedCount = updated == null ? 0 : updated;
		return updatedCount == 1;
	}

	private void waitBackoff() {
		try {
			Thread.sleep(dispatchPolicy.getRetryBackoff().toMillis());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private static String requireCreatedJobId(CreateProcessingJobResponse createdJob, Long linkId) {
		if (createdJob == null || createdJob.jobId() == null || createdJob.jobId().isBlank()) {
			throw new IllegalStateException("Processing job dispatch response does not contain jobId. linkId=" + linkId);
		}
		return createdJob.jobId();
	}
}

