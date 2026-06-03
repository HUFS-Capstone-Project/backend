package com.hufs.capstone.backend.course.api.response;

import com.hufs.capstone.backend.course.application.dto.MyDateCoursePageResult;
import java.util.List;

public record MyDateCoursePageResponse(
		List<MyDateCourseResponse> items,
		int page,
		int limit,
		long totalElements,
		int totalPages
) {

	public static MyDateCoursePageResponse from(MyDateCoursePageResult result) {
		return new MyDateCoursePageResponse(
				result.items().stream().map(MyDateCourseResponse::from).toList(),
				result.page(),
				result.limit(),
				result.totalElements(),
				result.totalPages()
		);
	}
}