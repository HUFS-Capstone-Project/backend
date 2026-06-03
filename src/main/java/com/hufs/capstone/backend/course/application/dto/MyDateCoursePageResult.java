package com.hufs.capstone.backend.course.application.dto;

import java.util.List;

public record MyDateCoursePageResult(
		List<MyDateCourseResult> items,
		int page,
		int limit,
		long totalElements,
		int totalPages
) {
}
