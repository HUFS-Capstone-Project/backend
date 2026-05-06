package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class PlaceBusinessHoursRefreshPolicy {

	public boolean shouldRequest(PlaceBusinessHours cache, Instant now) {
		if (cache == null) {
			return true;
		}
		if (isInProgressWithJob(cache)) {
			return false;
		}
		if (cache.getBusinessHoursExpiresAt() == null) {
			return true;
		}
		return !cache.getBusinessHoursExpiresAt().isAfter(now);
	}

	private static boolean isInProgressWithJob(PlaceBusinessHours cache) {
		return (cache.getBusinessHoursStatus() == BusinessHoursStatus.PENDING
				|| cache.getBusinessHoursStatus() == BusinessHoursStatus.FETCHING)
				&& cache.getBusinessHoursJobId() != null
				&& !cache.getBusinessHoursJobId().isBlank();
	}
}
