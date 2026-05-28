package com.hufs.capstone.backend.course.application.dto;

import java.math.BigDecimal;

public record DateCoursePlaceResult(
		Long roomPlaceId,
		Long placeId,
		String kakaoPlaceId,
		String name,
		String address,
		String roadAddress,
		BigDecimal latitude,
		BigDecimal longitude,
		String categoryCode,
		String categoryName,
		String tagCode,
		String tagName,
		int sequenceOrder
) {
}
