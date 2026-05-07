package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BusinessHoursJobCreateResponse(
		@JsonProperty("cache_hit")
		boolean cacheHit,
		@JsonProperty("job")
		BusinessHoursJobResponse job,
		@JsonProperty("place")
		BusinessHoursPlaceResponse place
) {
}
