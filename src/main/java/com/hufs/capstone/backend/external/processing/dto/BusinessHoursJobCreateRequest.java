package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BusinessHoursJobCreateRequest(
		@JsonProperty("kakao_place_id")
		String kakaoPlaceId,
		@JsonProperty("place_url")
		String placeUrl,
		@JsonProperty("place_name")
		String placeName
) {
}
