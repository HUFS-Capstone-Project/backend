package com.hufs.capstone.backend.course.application.dto;

import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import java.time.Instant;
import java.util.List;

public record DateCourseResult(
		String dateCourseId,
		CourseMode courseMode,
		String generationBatchId,
		Instant startDateTime,
		Instant endDateTime,
		Instant createdAt,
		List<DateCoursePlaceResult> places,
		List<Integer> skippedSlotIndices,
		Long savedByUserId,
		String savedByNickname,
		String savedByProfileImageUrl,
		Instant savedAt
) {
}
