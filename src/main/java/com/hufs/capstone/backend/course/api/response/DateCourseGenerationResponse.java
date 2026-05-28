package com.hufs.capstone.backend.course.api.response;

import com.hufs.capstone.backend.course.application.dto.DateCourseGenerationResult;
import java.util.List;

public record DateCourseGenerationResponse(
		String generationBatchId,
		List<DateCourseResponse> courses
) {

	public static DateCourseGenerationResponse from(DateCourseGenerationResult result) {
		return new DateCourseGenerationResponse(
				result.generationBatchId(),
				result.courses().stream().map(DateCourseResponse::from).toList()
		);
	}
}
