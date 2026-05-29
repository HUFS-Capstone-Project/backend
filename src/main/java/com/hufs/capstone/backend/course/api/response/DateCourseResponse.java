package com.hufs.capstone.backend.course.api.response;

import com.hufs.capstone.backend.course.application.dto.DateCourseResult;
import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import java.time.Instant;
import java.util.List;

public record DateCourseResponse(
		String publicId,
		CourseMode mode,
		String generationBatchId,
		Instant plannedDateTime,
		Instant createdAt,
		List<DateCoursePlaceResponse> places,
		List<Integer> skippedSlotIndices,
		Long savedByUserId,
		String savedByNickname,
		String savedByProfileImageUrl,
		Instant savedAt
) {

	public static DateCourseResponse from(DateCourseResult result) {
		return new DateCourseResponse(
				result.publicId(),
				result.courseMode(),
				result.generationBatchId(),
				result.plannedDateTime(),
				result.createdAt(),
				result.places().stream().map(DateCoursePlaceResponse::from).toList(),
				result.skippedSlotIndices(),
				result.savedByUserId(),
				result.savedByNickname(),
				result.savedByProfileImageUrl(),
				result.savedAt()
		);
	}
}
