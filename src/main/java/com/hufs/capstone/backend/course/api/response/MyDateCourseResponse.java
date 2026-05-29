package com.hufs.capstone.backend.course.api.response;

import com.hufs.capstone.backend.course.application.dto.MyDateCourseResult;
import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import java.time.Instant;
import java.util.List;

public record MyDateCourseResponse(
		String publicId,
		CourseMode mode,
		String generationBatchId,
		Instant plannedDateTime,
		Instant savedAt,
		String roomPublicId,
		String roomName,
		List<DateCoursePlaceResponse> places,
		List<Integer> skippedSlotIndices
) {

	public static MyDateCourseResponse from(MyDateCourseResult result) {
		return new MyDateCourseResponse(
				result.publicId(),
				result.courseMode(),
				result.generationBatchId(),
				result.plannedDateTime(),
				result.savedAt(),
				result.roomPublicId(),
				result.roomName(),
				result.places().stream().map(DateCoursePlaceResponse::from).toList(),
				result.skippedSlotIndices()
		);
	}
}