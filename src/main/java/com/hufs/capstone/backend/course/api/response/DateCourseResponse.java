package com.hufs.capstone.backend.course.api.response;

import com.hufs.capstone.backend.course.application.dto.DateCourseResult;
import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import java.time.Instant;
import java.util.List;

public record DateCourseResponse(
		String dateCourseId,
		CourseMode mode,
		String generationBatchId,
		Instant startDateTime,
		Instant endDateTime,
		Instant createdAt,
		List<DateCoursePlaceResponse> places,
		List<DateCourseCoordinateResponse> orderedCoordinates,
		List<Integer> skippedSlotIndices,
		Long savedByUserId,
		String savedByNickname,
		String savedByProfileImageUrl,
		Instant savedAt
) {

	public static DateCourseResponse from(DateCourseResult result) {
		return new DateCourseResponse(
				result.dateCourseId(),
				result.courseMode(),
				result.generationBatchId(),
				result.startDateTime(),
				result.endDateTime(),
				result.createdAt(),
				DateCourseResponseMapper.orderedPlaces(result.places()).stream()
						.map(DateCoursePlaceResponse::from)
						.toList(),
				DateCourseResponseMapper.orderedCoordinates(result.places()),
				result.skippedSlotIndices(),
				result.savedByUserId(),
				result.savedByNickname(),
				result.savedByProfileImageUrl(),
				result.savedAt()
		);
	}
}
