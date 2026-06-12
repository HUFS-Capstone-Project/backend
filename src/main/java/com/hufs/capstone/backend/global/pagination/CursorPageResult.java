package com.hufs.capstone.backend.global.pagination;

import java.util.List;

public record CursorPageResult<T>(
		List<T> items,
		int limit,
		long totalCount,
		String nextCursor,
		boolean hasNext
) {
}
