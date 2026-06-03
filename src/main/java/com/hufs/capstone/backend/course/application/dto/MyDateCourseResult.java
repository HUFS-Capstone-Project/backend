package com.hufs.capstone.backend.course.application.dto;

import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import java.time.Instant;
import java.util.List;

public record MyDateCourseResult(
		String dateCourseId,
		CourseMode courseMode,
		String generationBatchId,
		Instant startDateTime,
		Instant endDateTime,
		Instant savedAt,
		String roomPublicId,
		String roomName,
		List<DateCoursePlaceResult> places,
		List<Integer> skippedSlotIndices
) {
}
