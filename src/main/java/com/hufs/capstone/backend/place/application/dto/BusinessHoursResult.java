package com.hufs.capstone.backend.place.application.dto;

import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import java.time.Instant;

public record BusinessHoursResult(
		BusinessHoursDisplayResult businessHours,
		BusinessHoursStatus businessHoursStatus,
		Instant businessHoursFetchedAt,
		Instant businessHoursExpiresAt
) {
}
