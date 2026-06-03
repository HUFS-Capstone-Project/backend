package com.hufs.capstone.backend.course.api.response;

import com.hufs.capstone.backend.course.application.dto.MyDateCourseResult;
import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import java.time.Instant;
import java.util.List;

public record MyDateCourseResponse(
		String dateCourseId,
		CourseMode mode,
		String generationBatchId,
		Instant startDateTime,
		Instant endDateTime,
		Instant savedAt,
		String roomPublicId,
		String roomName,
		List<DateCoursePlaceResponse> places,
		List<DateCourseCoordinateResponse> orderedCoordinates,
		List<Integer> skippedSlotIndices
) {

	public static MyDateCourseResponse from(MyDateCourseResult result) {
		return new MyDateCourseResponse(
				result.dateCourseId(),
				result.courseMode(),
				result.generationBatchId(),
				result.startDateTime(),
				result.endDateTime(),
				result.savedAt(),
				result.roomPublicId(),
				result.roomName(),
				DateCourseResponseMapper.orderedPlaces(result.places()).stream()
						.map(DateCoursePlaceResponse::from)
						.toList(),
				DateCourseResponseMapper.orderedCoordinates(result.places()),
				result.skippedSlotIndices()
		);
	}
}
