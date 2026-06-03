package com.hufs.capstone.backend.course.api.response;

import com.hufs.capstone.backend.course.application.dto.DateCoursePlaceResult;
import java.math.BigDecimal;

public record DateCoursePlaceResponse(
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

	public static DateCoursePlaceResponse from(DateCoursePlaceResult result) {
		return new DateCoursePlaceResponse(
				result.roomPlaceId(),
				result.placeId(),
				result.kakaoPlaceId(),
				result.name(),
				result.address(),
				result.roadAddress(),
				result.latitude(),
				result.longitude(),
				result.categoryCode(),
				result.categoryName(),
				result.tagCode(),
				result.tagName(),
				result.sequenceOrder()
		);
	}
}
