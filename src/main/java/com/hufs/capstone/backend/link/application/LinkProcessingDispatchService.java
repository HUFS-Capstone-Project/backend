package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.external.processing.InstagramRateLimitedException;
import com.hufs.capstone.backend.external.processing.ProcessingClient;
import com.hufs.capstone.backend.external.processing.ProcessingErrorCodes;
import com.hufs.capstone.backend.external.processing.dto.CreateProcessingJobResponse;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
import com.hufs.capstone.backend.link.domain.entity.LinkProcessingDispatchAttempt;
import com.hufs.capstone.backend.link.domain.entity.LinkProcessingHistory;
import com.hufs.capstone.backend.link.domain.repository.LinkProcessingDispatchAttemptRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkProcessingHistoryRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import java.time.Instant;
import java.util.UUID;
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
	private static final String IDEMPOTENCY_KEY_PREFIX = "link-dispatch-attempt:";
	private static final String DISPATCH_FAILED_ERROR_MESSAGE = "처리 디스패치 재시도가 모두 소진되었습니다.";
	private static final String INSTAGRAM_RATE_LIMITED_USER_MESSAGE =
			"현재 Instagram 분석이 일시적으로 제한되어 있어요. 잠시 후 다시 시도해 주세요.";

	private final ProcessingClient processingClient;
	private final LinkRepository linkRepository;
	private final LinkProcessingDispatchAttemptRepository dispatchAttemptRepository;
	private final LinkProcessingHistoryRepository linkProcessingHistoryRepository;
	private final LinkProcessingDispatchPolicy dispatchPolicy;
	private final PlatformTransactionManager transactionManager;

	public void dispatch(Long dispatchAttemptId) {
		DispatchPayload payload = findActivePayload(dispatchAttemptId);
		if (payload == null) {
			log.debug("활성 processing dispatch 시도가 없어 건너뜁니다. attemptId={}", dispatchAttemptId);
			return;
		}

		String claimToken = claimDispatch(payload);
		if (claimToken == null) {
			log.debug(
					"이미 다른 실행자가 처리 중이어서 processing dispatch를 건너뜁니다. attemptId={}, linkId={}",
					dispatchAttemptId,
					payload.linkId()
			);
			return;
		}

		RuntimeException lastException = null;
		for (int attempt = 1; attempt <= dispatchPolicy.getMaxAttempts(); attempt++) {
			try {
				CreateProcessingJobResponse createdJob = processingClient.createJob(
						payload.originalUrl(),
						payload.roomId(),
						formatIdempotencyKey(payload.idempotencyKey())
				);
				String createdJobId = requireCreatedJobId(createdJob, payload.linkId());
				bindCreatedJobId(payload, claimToken, createdJobId);
				return;
			} catch (InstagramRateLimitedException ex) {
				log.warn(
						"Instagram rate limit으로 처리 디스패치를 최종 실패 처리합니다. attemptId={}, linkId={}",
						dispatchAttemptId,
						payload.linkId(),
						ex
				);
				handleInstagramRateLimited(payload, claimToken, ex);
				return;
			} catch (RuntimeException ex) {
				lastException = ex;
				log.warn(
						"처리 디스패치에 실패했습니다. attemptId={}, linkId={}, attempt={}/{}",
						dispatchAttemptId,
						payload.linkId(),
						attempt,
						dispatchPolicy.getMaxAttempts(),
						ex
				);
				if (attempt < dispatchPolicy.getMaxAttempts() && !waitBackoff()) {
					break;
				}
			}
		}

		handleExhaustedRetry(payload, claimToken, lastException);
	}

	private DispatchPayload findActivePayload(Long dispatchAttemptId) {
		return dispatchAttemptRepository.findActiveById(dispatchAttemptId)
				.map(attempt -> new DispatchPayload(
						attempt.getId(),
						attempt.getLink().getId(),
						attempt.getOriginalUrl(),
						attempt.getRoomId(),
						attempt.getIdempotencyKey()
				))
				.orElse(null);
	}

	private String claimDispatch(DispatchPayload payload) {
		Instant now = Instant.now();
		Instant staleBefore = now.minus(dispatchPolicy.getStaleThreshold());
		String claimToken = UUID.randomUUID().toString();
		TransactionTemplate transactionTemplate = requiresNewTransaction();
		Boolean claimed = transactionTemplate.execute(status -> {
			LinkProcessingDispatchAttempt attempt = dispatchAttemptRepository
					.findByIdForUpdate(payload.attemptId())
					.orElse(null);
			if (attempt == null || !attempt.claim(claimToken, now, staleBefore)) {
				return false;
			}
			int updated = linkRepository.claimDispatchForProcessing(
					payload.linkId(),
					LinkAnalysisStatus.REQUESTED,
					ProcessingDispatchStatus.PENDING,
					ProcessingDispatchStatus.DISPATCHING,
					ProcessingDispatchStatus.DISPATCHING,
					staleBefore,
					now
			);
			if (updated != 1) {
				status.setRollbackOnly();
				return false;
			}
			return true;
		});
		return Boolean.TRUE.equals(claimed) ? claimToken : null;
	}

	private void bindCreatedJobId(DispatchPayload payload, String claimToken, String createdJobId) {
		TransactionTemplate transactionTemplate = requiresNewTransaction();
		Boolean bound = transactionTemplate.execute(status -> {
			LinkProcessingDispatchAttempt attempt = dispatchAttemptRepository
					.findByIdForUpdate(payload.attemptId())
					.orElse(null);
			if (attempt == null || !attempt.isOwnedBy(claimToken)) {
				return false;
			}
			Instant now = Instant.now();
			int updated = linkRepository.bindProcessingJobIdForPending(
					payload.linkId(),
					createdJobId,
					ProcessingDispatchStatus.DISPATCHING,
					ProcessingDispatchStatus.DISPATCHED,
					now
			);
			if (updated != 1 || !attempt.markDispatched(claimToken, createdJobId, now)) {
				status.setRollbackOnly();
				return false;
			}
			return true;
		});
		if (Boolean.TRUE.equals(bound)) {
			log.info(
					"처리 작업 디스패치를 완료했습니다. attemptId={}, linkId={}, processingJobId={}",
					payload.attemptId(),
					payload.linkId(),
					createdJobId
			);
			return;
		}
		log.info(
				"디스패치 소유권이나 링크 상태가 변경되어 작업 바인딩을 건너뜁니다. attemptId={}, linkId={}",
				payload.attemptId(),
				payload.linkId()
		);
	}

	private void handleInstagramRateLimited(
			DispatchPayload payload,
			String claimToken,
			InstagramRateLimitedException exception
	) {
		boolean markedFailed = markDispatchFailedIfOwned(
				payload,
				claimToken,
				LinkAnalysisStatus.FAILED,
				ProcessingErrorCodes.INSTAGRAM_RATE_LIMITED,
				INSTAGRAM_RATE_LIMITED_USER_MESSAGE,
				exception.isRetryable(),
				exception.getCooldownSeconds()
		);
		if (!markedFailed) {
			log.info(
					"디스패치 소유권이나 링크 상태가 변경되어 Instagram 실패 처리를 건너뜁니다. attemptId={}, linkId={}",
					payload.attemptId(),
					payload.linkId()
			);
			return;
		}
		saveDispatchFailedHistory(
				payload.linkId(),
				ProcessingErrorCodes.INSTAGRAM_RATE_LIMITED,
				INSTAGRAM_RATE_LIMITED_USER_MESSAGE
		);
	}

	private void handleExhaustedRetry(DispatchPayload payload, String claimToken, RuntimeException lastException) {
		boolean markedFailed = markDispatchFailedIfOwned(
				payload,
				claimToken,
				LinkAnalysisStatus.DISPATCH_FAILED,
				DISPATCH_FAILED_ERROR_CODE,
				DISPATCH_FAILED_ERROR_MESSAGE,
				null,
				null
		);
		if (!markedFailed) {
			log.info(
					"디스패치 소유권이나 링크 상태가 변경되어 재시도 소진 처리를 건너뜁니다. attemptId={}, linkId={}",
					payload.attemptId(),
					payload.linkId()
			);
			return;
		}

		log.error(
				"처리 디스패치 재시도가 모두 소진되어 DISPATCH_FAILED 상태로 전환합니다. attemptId={}, linkId={}",
				payload.attemptId(),
				payload.linkId(),
				lastException
		);
		saveDispatchFailedHistory(payload.linkId(), DISPATCH_FAILED_ERROR_CODE, DISPATCH_FAILED_ERROR_MESSAGE);
	}

	private boolean markDispatchFailedIfOwned(
			DispatchPayload payload,
			String claimToken,
			LinkAnalysisStatus targetStatus,
			String errorCode,
			String errorMessage,
			Boolean retryable,
			Integer cooldownSeconds
	) {
		TransactionTemplate transactionTemplate = requiresNewTransaction();
		Boolean marked = transactionTemplate.execute(status -> {
			LinkProcessingDispatchAttempt attempt = dispatchAttemptRepository
					.findByIdForUpdate(payload.attemptId())
					.orElse(null);
			if (attempt == null || !attempt.isOwnedBy(claimToken)) {
				return false;
			}
			Instant now = Instant.now();
			int updated = linkRepository.markDispatchFailedIfNoJob(
					payload.linkId(),
					ProcessingDispatchStatus.DISPATCHING,
					ProcessingDispatchStatus.DISPATCH_FAILED,
					targetStatus,
					errorCode,
					errorMessage,
					retryable,
					cooldownSeconds,
					now
			);
			if (updated != 1 || !attempt.markFailed(claimToken, now)) {
				status.setRollbackOnly();
				return false;
			}
			return true;
		});
		return Boolean.TRUE.equals(marked);
	}

	private void saveDispatchFailedHistory(Long linkId, String errorCode, String errorMessage) {
		TransactionTemplate transactionTemplate = requiresNewTransaction();
		transactionTemplate.executeWithoutResult(status -> linkRepository.findById(linkId)
				.ifPresent(link -> linkProcessingHistoryRepository.save(LinkProcessingHistory.dispatchFailed(
						link,
						errorCode,
						errorMessage
				))));
	}

	private TransactionTemplate requiresNewTransaction() {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return transactionTemplate;
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

	private static String formatIdempotencyKey(UUID idempotencyKey) {
		if (idempotencyKey == null) {
			throw new IllegalStateException("Dispatch attempt does not contain idempotencyKey");
		}
		return IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
	}

	private record DispatchPayload(
			Long attemptId,
			Long linkId,
			String originalUrl,
			String roomId,
			UUID idempotencyKey
	) {
	}
}
