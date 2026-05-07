package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BusinessHoursPlaceResponse(
		@JsonProperty("kakao_place_id")
		String kakaoPlaceId,
		@JsonProperty("place_name")
		String placeName,
		@JsonProperty("place_url")
		String placeUrl,
		@JsonProperty("business_hours_status")
		BusinessHoursStatus businessHoursStatus,
		@JsonProperty("business_hours")
		JsonNode businessHours,
		@JsonProperty("business_hours_fetched_at")
		OffsetDateTime businessHoursFetchedAt,
		@JsonProperty("business_hours_expires_at")
		OffsetDateTime businessHoursExpiresAt,
		@JsonProperty("error_code")
		String errorCode,
		@JsonProperty("error_message")
		String errorMessage
) {
}
