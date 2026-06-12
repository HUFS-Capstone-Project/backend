package com.hufs.capstone.backend.place.api.response;

import com.hufs.capstone.backend.global.pagination.CursorPageResult;
import com.hufs.capstone.backend.place.application.dto.MyRoomPlaceResult;
import java.util.List;

public record MyRoomPlaceCursorPageResponse(
		List<MyRoomPlaceResponse> items,
		int limit,
		long totalCount,
		String nextCursor,
		boolean hasNext
) {

	public static MyRoomPlaceCursorPageResponse from(CursorPageResult<MyRoomPlaceResult> result) {
		return new MyRoomPlaceCursorPageResponse(
				result.items().stream()
						.map(MyRoomPlaceResponse::from)
						.toList(),
				result.limit(),
				result.totalCount(),
				result.nextCursor(),
				result.hasNext()
		);
	}
}
