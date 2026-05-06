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
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
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
		if (!businessHoursProperties.polling().enabled()) {
			return;
		}
		Instant now = Instant.now();
		Instant dueBefore = now.minus(businessHoursProperties.polling().interval());
		List<PlaceBusinessHours> pollableRows = placeBusinessHoursRepository.findPollable(
				POLLABLE_STATUSES,
				dueBefore,
				PageRequest.of(0, businessHoursProperties.polling().batchSize())
		);
		for (PlaceBusinessHours row : pollableRows) {
			pollOne(row, now);
		}
	}

	private void pollOne(PlaceBusinessHours row, Instant now) {
		placeBusinessHoursCacheService.markPolled(row.getId(), now);
		try {
			BusinessHoursJobLookupResponse response = processingBusinessHoursClient.getJob(row.getBusinessHoursJobId());
			if (response == null || response.job() == null || response.job().status() == null
					|| isRunning(response.job().status())) {
				return;
			}
			if (response.place() != null) {
				placeBusinessHoursCacheService.upsertRemotePlace(response.place(), BusinessHoursRequestStatus.SUCCEEDED);
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
					BusinessHoursRequestStatus.REQUEST_FAILED,
					ex.getProcessingErrorCode() == null ? ex.getMessage() : ex.getProcessingErrorCode()
			);
		} catch (RuntimeException ex) {
			log.warn("Business hours polling failed. kakaoPlaceId={}", row.getKakaoPlaceId(), ex);
			placeBusinessHoursCacheService.markRequestFailure(
					toEventSnapshot(row),
					BusinessHoursRequestStatus.REQUEST_FAILED,
					ex.getMessage()
			);
		}
	}

	private static boolean isRunning(BusinessHoursJobStatus status) {
		return status == BusinessHoursJobStatus.PENDING || status == BusinessHoursJobStatus.FETCHING;
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
