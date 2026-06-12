package com.hufs.capstone.backend.course.api.response;

import com.hufs.capstone.backend.course.application.dto.DateCourseResult;
import com.hufs.capstone.backend.global.pagination.CursorPageResult;
import java.util.List;

public record DateCourseCursorPageResponse(
		List<DateCourseResponse> items,
		int limit,
		long totalCount,
		String nextCursor,
		boolean hasNext
) {

	public static DateCourseCursorPageResponse from(CursorPageResult<DateCourseResult> result) {
		return new DateCourseCursorPageResponse(
				result.items().stream()
						.map(DateCourseResponse::from)
						.toList(),
				result.limit(),
				result.totalCount(),
				result.nextCursor(),
				result.hasNext()
		);
	}
}
