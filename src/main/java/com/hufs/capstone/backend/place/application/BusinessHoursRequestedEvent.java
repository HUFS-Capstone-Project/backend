package com.hufs.capstone.backend.place.application;

import java.time.LocalDate;

public record BusinessHoursRequestedEvent(
		Long roomPlaceId,
		Long placeId,
		String kakaoPlaceId,
		String placeUrl,
		String placeName,
		LocalDate requiredDate,
		boolean created,
		String refreshReason
) {
}
