package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BusinessHoursJobResponse(
		String jobId,
		String kakaoPlaceId,
		String placeUrl,
		BusinessHoursJobStatus status,
		String errorCode,
		String errorMessage,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
