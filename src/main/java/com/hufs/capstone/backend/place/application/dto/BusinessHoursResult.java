package com.hufs.capstone.backend.place.application.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import java.time.Instant;

public record BusinessHoursResult(
		JsonNode businessHours,
		String businessHoursRaw,
		BusinessHoursStatus businessHoursStatus,
		Instant businessHoursFetchedAt,
		Instant businessHoursExpiresAt,
		String businessHoursSource
) {
}
