package com.hufs.capstone.backend.place.api.response;

import com.hufs.capstone.backend.global.pagination.CursorPageResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceResult;
import java.util.List;

public record RoomPlaceCursorPageResponse(
		List<RoomPlaceResponse> items,
		int limit,
		long totalCount,
		String nextCursor,
		boolean hasNext
) {

	public static RoomPlaceCursorPageResponse from(CursorPageResult<RoomPlaceResult> result) {
		return new RoomPlaceCursorPageResponse(
				result.items().stream()
						.map(RoomPlaceResponse::from)
						.toList(),
				result.limit(),
				result.totalCount(),
				result.nextCursor(),
				result.hasNext()
		);
	}
}
