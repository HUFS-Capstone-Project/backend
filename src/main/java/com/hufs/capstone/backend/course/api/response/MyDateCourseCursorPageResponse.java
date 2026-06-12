package com.hufs.capstone.backend.course.api.response;

import com.hufs.capstone.backend.course.application.dto.MyDateCourseResult;
import com.hufs.capstone.backend.global.pagination.CursorPageResult;
import java.util.List;

public record MyDateCourseCursorPageResponse(
		List<MyDateCourseResponse> items,
		int limit,
		long totalCount,
		String nextCursor,
		boolean hasNext
) {

	public static MyDateCourseCursorPageResponse from(CursorPageResult<MyDateCourseResult> result) {
		return new MyDateCourseCursorPageResponse(
				result.items().stream()
						.map(MyDateCourseResponse::from)
						.toList(),
				result.limit(),
				result.totalCount(),
				result.nextCursor(),
				result.hasNext()
		);
	}
}
