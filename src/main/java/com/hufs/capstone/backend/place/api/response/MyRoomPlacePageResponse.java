package com.hufs.capstone.backend.place.api.response;

import com.hufs.capstone.backend.place.application.dto.MyRoomPlacePageResult;
import java.util.List;

public record MyRoomPlacePageResponse(
		List<MyRoomPlaceResponse> items,
		int page,
		int limit,
		long totalElements,
		int totalPages
) {

	public static MyRoomPlacePageResponse from(MyRoomPlacePageResult result) {
		return new MyRoomPlacePageResponse(
				result.items().stream()
						.map(MyRoomPlaceResponse::from)
						.toList(),
				result.page(),
				result.limit(),
				result.totalElements(),
				result.totalPages()
		);
	}
}
