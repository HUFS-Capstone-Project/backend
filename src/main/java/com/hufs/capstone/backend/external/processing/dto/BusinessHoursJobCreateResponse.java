package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BusinessHoursJobCreateResponse(
		BusinessHoursJobResponse job,
		BusinessHoursPlaceResponse place,
		boolean created,
		boolean enqueued,
		boolean cacheHit
) {
}
