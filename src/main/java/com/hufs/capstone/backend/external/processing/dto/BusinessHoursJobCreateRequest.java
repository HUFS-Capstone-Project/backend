package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record BusinessHoursJobCreateRequest(
		@JsonProperty("kakao_place_id")
		String kakaoPlaceId,
		@JsonProperty("place_url")
		String placeUrl,
		@JsonProperty("place_name")
		String placeName,
		@JsonFormat(shape = JsonFormat.Shape.STRING)
		@JsonProperty("required_date")
		LocalDate requiredDate
) {
}
