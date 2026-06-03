package com.hufs.capstone.backend.course.api.response;

import com.hufs.capstone.backend.course.application.dto.DateCoursePlaceResult;
import java.math.BigDecimal;

public record DateCourseCoordinateResponse(
		int sequenceOrder,
		BigDecimal latitude,
		BigDecimal longitude
) {

	public static DateCourseCoordinateResponse from(DateCoursePlaceResult place) {
		return new DateCourseCoordinateResponse(
				place.sequenceOrder(),
				place.latitude(),
				place.longitude()
		);
	}
}
