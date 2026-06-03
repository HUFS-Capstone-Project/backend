package com.hufs.capstone.backend.course.api.response;

import com.hufs.capstone.backend.course.application.dto.DateCoursePlaceResult;
import java.util.Comparator;
import java.util.List;

final class DateCourseResponseMapper {

	private DateCourseResponseMapper() {
	}

	static List<DateCoursePlaceResult> orderedPlaces(List<DateCoursePlaceResult> places) {
		return places.stream()
				.sorted(Comparator.comparingInt(DateCoursePlaceResult::sequenceOrder))
				.toList();
	}

	static List<DateCourseCoordinateResponse> orderedCoordinates(List<DateCoursePlaceResult> places) {
		return orderedPlaces(places).stream()
				.filter(place -> place.latitude() != null && place.longitude() != null)
				.map(DateCourseCoordinateResponse::from)
				.toList();
	}
}
