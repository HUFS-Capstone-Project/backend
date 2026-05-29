package com.hufs.capstone.backend.course.api.response;

import com.hufs.capstone.backend.course.application.dto.DateCoursePageResult;
import java.util.List;

public record DateCoursePageResponse(
		List<DateCourseResponse> items,
		int page,
		int limit,
		long totalElements,
		int totalPages
) {

	public static DateCoursePageResponse from(DateCoursePageResult result) {
		return new DateCoursePageResponse(
				result.items().stream().map(DateCourseResponse::from).toList(),
				result.page(),
				result.limit(),
				result.totalElements(),
				result.totalPages()
		);
	}
}
