package com.hufs.capstone.backend.place.application;

public record BusinessHoursRequestedEvent(
		Long roomPlaceId,
		Long placeId,
		String kakaoPlaceId,
		String placeUrl,
		String placeName,
		boolean created,
		String refreshReason
) {
}
