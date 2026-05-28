package com.hufs.capstone.backend.course.application.dto;

import java.util.List;

public record DateCourseGenerationResult(
		String generationBatchId,
		List<DateCourseResult> courses
) {
}
