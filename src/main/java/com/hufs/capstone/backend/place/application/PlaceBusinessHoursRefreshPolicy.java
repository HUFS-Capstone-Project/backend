package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class PlaceBusinessHoursRefreshPolicy {
	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	public boolean shouldRequestInitially(PlaceBusinessHours cache) {
		return cache == null;
	}

	public boolean shouldRequestForDetail(
			PlaceBusinessHours cache,
			LocalDate requiredDate,
			Instant now,
			BusinessHoursProperties.DetailRefresh settings
	) {
		if (cache == null) {
			return true;
		}
		if (isInProgress(cache)) {
			return isUndispatchedRequestTimedOut(cache, now, settings);
		}
		if (wasFetchedOn(cache, requiredDate)) {
			return false;
		}
		return cache.getBusinessHoursStatus() != BusinessHoursStatus.FAILED
				|| cache.getBusinessHoursExpiresAt() == null
				|| !cache.getBusinessHoursExpiresAt().isAfter(now);
	}

	private static boolean isInProgress(PlaceBusinessHours cache) {
		return cache.getBusinessHoursStatus() == BusinessHoursStatus.PENDING
				|| cache.getBusinessHoursStatus() == BusinessHoursStatus.FETCHING;
	}

	private static boolean isUndispatchedRequestTimedOut(
			PlaceBusinessHours cache,
			Instant now,
			BusinessHoursProperties.DetailRefresh settings
	) {
		if (cache.getBusinessHoursJobId() != null && !cache.getBusinessHoursJobId().isBlank()) {
			return cache.getBusinessHoursExpiresAt() != null
					&& !cache.getBusinessHoursExpiresAt().isAfter(now);
		}
		return cache.getLastPolledAt() == null
				|| !cache.getLastPolledAt().plus(settings.dispatchTimeout()).isAfter(now);
	}

	private static boolean wasFetchedOn(PlaceBusinessHours cache, LocalDate requiredDate) {
		return cache.getBusinessHoursFetchedAt() != null
				&& cache.getBusinessHoursFetchedAt().atZone(SEOUL_ZONE).toLocalDate().equals(requiredDate);
	}
}
