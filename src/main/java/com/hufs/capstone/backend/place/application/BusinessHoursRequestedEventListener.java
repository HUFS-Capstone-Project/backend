package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.external.processing.ProcessingBusinessHoursClient;
import com.hufs.capstone.backend.external.processing.ProcessingClientErrorType;
import com.hufs.capstone.backend.external.processing.ProcessingClientException;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursJobCreateRequest;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursJobCreateResponse;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursRequestStatus;
import com.hufs.capstone.backend.place.infrastructure.config.BusinessHoursAsyncConfig;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessHoursRequestedEventListener {

	private static final String KAKAO_PLACE_HOST = "place.map.kakao.com";

	private final ProcessingBusinessHoursClient processingBusinessHoursClient;
	private final PlaceBusinessHoursCacheService placeBusinessHoursCacheService;

	@Async(BusinessHoursAsyncConfig.BUSINESS_HOURS_TASK_EXECUTOR)
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onBusinessHoursRequested(BusinessHoursRequestedEvent event) {
		if (!isRequestable(event)) {
			log.debug(
					"Business hours request skipped. roomPlaceId={}, kakaoPlaceId={}",
					event.roomPlaceId(),
					event.kakaoPlaceId()
			);
			return;
		}
		try {
			BusinessHoursJobCreateResponse response = processingBusinessHoursClient.createJob(
					new BusinessHoursJobCreateRequest(event.kakaoPlaceId(), event.placeUrl(), event.placeName())
			);
			applyCreateResponse(event, response);
		} catch (ProcessingClientException ex) {
			handleProcessingClientException(event, ex);
		} catch (RuntimeException ex) {
			log.warn(
					"Business hours request failed. roomPlaceId={}, kakaoPlaceId={}",
					event.roomPlaceId(),
					event.kakaoPlaceId(),
					ex
			);
			placeBusinessHoursCacheService.markRequestFailure(
					event,
					BusinessHoursRequestStatus.FAILED,
					ex.getMessage()
			);
		}
	}

	private void applyCreateResponse(BusinessHoursRequestedEvent event, BusinessHoursJobCreateResponse response) {
		if (response == null || response.place() == null) {
			placeBusinessHoursCacheService.markRequestFailure(
					event,
					BusinessHoursRequestStatus.FAILED,
					"Business hours response does not contain place."
			);
			return;
		}
		if (response.cacheHit() && response.place().businessHoursExpiresAt() == null) {
			log.info(
					"Business hours cache_hit response has null expiresAt. kakaoPlaceId={}",
					response.place().kakaoPlaceId()
			);
		}
		placeBusinessHoursCacheService.upsertRemotePlace(
				response.place(),
				response.job() == null ? null : response.job().jobId(),
				BusinessHoursRequestStatus.SUCCEEDED
		);
	}

	private void handleProcessingClientException(BusinessHoursRequestedEvent event, ProcessingClientException ex) {
		if (ex.hasStatus(422)) {
			placeBusinessHoursCacheService.markRequestFailure(
					event,
					BusinessHoursRequestStatus.INVALID_REQUEST,
					errorMessage(ex)
			);
			return;
		}
		if (ex.hasStatus(503) && isEnqueueFailed(ex)) {
			placeBusinessHoursCacheService.markFailed(event, errorMessage(ex));
			return;
		}
		BusinessHoursRequestStatus status = ex.getErrorType() == ProcessingClientErrorType.CLIENT_ERROR
				? BusinessHoursRequestStatus.INVALID_REQUEST
				: BusinessHoursRequestStatus.FAILED;
		placeBusinessHoursCacheService.markRequestFailure(event, status, errorMessage(ex));
	}

	private static boolean isRequestable(BusinessHoursRequestedEvent event) {
		return hasText(event.kakaoPlaceId())
				&& hasText(event.placeUrl())
				&& hasKakaoPlaceHost(event.placeUrl());
	}

	private static boolean hasKakaoPlaceHost(String placeUrl) {
		try {
			URI uri = URI.create(placeUrl.trim());
			return KAKAO_PLACE_HOST.equalsIgnoreCase(uri.getHost());
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private static boolean isEnqueueFailed(ProcessingClientException ex) {
		return ex.getProcessingErrorCode() == null
				|| "ENQUEUE_FAILED".equals(ex.getProcessingErrorCode())
				|| "FAILED".equals(ex.getProcessingErrorCode());
	}

	private static String errorMessage(ProcessingClientException ex) {
		if (hasText(ex.getProcessingErrorCode())) {
			return ex.getProcessingErrorCode();
		}
		return ex.getMessage();
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
