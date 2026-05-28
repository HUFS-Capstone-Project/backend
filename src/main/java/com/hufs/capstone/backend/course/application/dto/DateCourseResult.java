package com.hufs.capstone.backend.course.application.dto;

import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import java.time.Instant;
import java.util.List;

public record DateCourseResult(
		String publicId,
		CourseMode courseMode,
		String generationBatchId,
		Instant plannedDateTime,
		Instant createdAt,
		List<DateCoursePlaceResult> places,
		List<Integer> skippedSlotIndices
) {
}
