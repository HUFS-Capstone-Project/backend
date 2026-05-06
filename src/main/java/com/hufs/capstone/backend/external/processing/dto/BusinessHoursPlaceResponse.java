package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BusinessHoursPlaceResponse(
		String kakaoPlaceId,
		String placeUrl,
		String placeName,
		JsonNode businessHours,
		String businessHoursRaw,
		BusinessHoursStatus businessHoursStatus,
		OffsetDateTime businessHoursFetchedAt,
		OffsetDateTime businessHoursExpiresAt,
		String businessHoursSource,
		String businessHoursJobId,
		String lastError,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt,
		Long version
) {
}
