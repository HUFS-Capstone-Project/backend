package com.hufs.capstone.backend.course.api.response;

import com.hufs.capstone.backend.course.application.dto.DateCourseBatchResult;
import java.time.Instant;
import java.util.List;

public record DateCourseBatchResponse(
		String generationBatchId,
		Instant createdAt,
		Instant plannedDateTime,
		List<DateCourseResponse> courses
) {

	public static DateCourseBatchResponse from(DateCourseBatchResult result) {
		return new DateCourseBatchResponse(
				result.generationBatchId(),
				result.createdAt(),
				result.plannedDateTime(),
				result.courses().stream().map(DateCourseResponse::from).toList()
		);
	}
}
