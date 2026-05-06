package com.hufs.capstone.backend.external.processing.dto;

public record BusinessHoursJobCreateRequest(
		String kakaoPlaceId,
		String placeUrl,
		String placeName
) {
}
