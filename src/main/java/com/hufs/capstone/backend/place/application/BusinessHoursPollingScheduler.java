package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.external.processing.ProcessingBusinessHoursClient;
import com.hufs.capstone.backend.external.processing.ProcessingClientException;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursJobLookupResponse;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursJobStatus;
import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursRequestStatus;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import com.hufs.capstone.backend.place.domain.repository.PlaceBusinessHoursRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.business-hours.polling", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class BusinessHoursPollingScheduler {

	private static final EnumSet<BusinessHoursStatus> POLLABLE_STATUSES =
			EnumSet.of(BusinessHoursStatus.PENDING, BusinessHoursStatus.FETCHING);

	private final BusinessHoursProperties businessHoursProperties;
	private final PlaceBusinessHoursRepository placeBusinessHoursRepository;
	private final ProcessingBusinessHoursClient processingBusinessHoursClient;
	private final PlaceBusinessHoursCacheService placeBusinessHoursCacheService;

	@Scheduled(fixedDelayString = "${app.business-hours.polling.scheduler-delay-ms:30000}")
	public void pollBusinessHoursJobs() {
		Instant now = Instant.now();
		Instant dueBefore = now.minus(businessHoursProperties.polling().interval());
		List<PlaceBusinessHours> pollableRows = placeBusinessHoursRepository.findPollable(
				POLLABLE_STATUSES,
				dueBefore,
				PageRequest.of(0, businessHoursProperties.polling().batchSize())
		);
		for (PlaceBusinessHours row : pollableRows) {
			pollOne(row, now, dueBefore);
		}
	}

	private void pollOne(PlaceBusinessHours row, Instant now, Instant dueBefore) {
		if (!placeBusinessHoursCacheService.claimPolling(row.getId(), POLLABLE_STATUSES, dueBefore, now)) {
			return;
		}
		try {
			BusinessHoursJobLookupResponse response = processingBusinessHoursClient.getJob(row.getBusinessHoursJobId());
			if (response == null || response.job() == null || response.job().status() == null
					|| isRunning(response.job().status())) {
				return;
			}
			if (response.job().status() == BusinessHoursJobStatus.FAILED) {
				placeBusinessHoursCacheService.markFailed(toEventSnapshot(row), jobFailureMessage(response));
				return;
			}
			if (response.place() != null) {
				placeBusinessHoursCacheService.upsertRemotePlace(
						response.place(),
						response.job().jobId(),
						BusinessHoursRequestStatus.SUCCEEDED
				);
				return;
			}
			processingBusinessHoursClient.getPlace(row.getKakaoPlaceId())
					.ifPresent(place -> placeBusinessHoursCacheService.upsertRemotePlace(
							place,
							BusinessHoursRequestStatus.SUCCEEDED
					));
		} catch (ProcessingClientException ex) {
			placeBusinessHoursCacheService.markRequestFailure(
					toEventSnapshot(row),
					BusinessHoursRequestStatus.FAILED,
					ex.getProcessingErrorCode() == null ? ex.getMessage() : ex.getProcessingErrorCode()
			);
		} catch (RuntimeException ex) {
			log.warn("Business hours polling failed. kakaoPlaceId={}", row.getKakaoPlaceId(), ex);
			placeBusinessHoursCacheService.markRequestFailure(
					toEventSnapshot(row),
					BusinessHoursRequestStatus.FAILED,
					ex.getMessage()
			);
		}
	}

	private static boolean isRunning(BusinessHoursJobStatus status) {
		return status == BusinessHoursJobStatus.QUEUED || status == BusinessHoursJobStatus.PROCESSING;
	}

	private static String jobFailureMessage(BusinessHoursJobLookupResponse response) {
		String errorCode = response.job().errorCode();
		String errorMessage = response.job().errorMessage();
		if (errorCode != null && !errorCode.isBlank() && errorMessage != null && !errorMessage.isBlank()) {
			return errorCode + ": " + errorMessage;
		}
		if (errorCode != null && !errorCode.isBlank()) {
			return errorCode;
		}
		return errorMessage;
	}

	private static BusinessHoursRequestedEvent toEventSnapshot(PlaceBusinessHours row) {
		return new BusinessHoursRequestedEvent(
				null,
				null,
				row.getKakaoPlaceId(),
				row.getPlaceUrl(),
				row.getPlaceName(),
				false,
				"polling"
		);
	}
}
