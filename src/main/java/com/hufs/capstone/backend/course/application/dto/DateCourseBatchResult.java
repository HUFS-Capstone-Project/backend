package com.hufs.capstone.backend.course.application.dto;

import java.time.Instant;
import java.util.List;

public record DateCourseBatchResult(
		String generationBatchId,
		Instant createdAt,
		Instant plannedDateTime,
		List<DateCourseResult> courses
) {
}
