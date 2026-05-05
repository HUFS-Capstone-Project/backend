package com.hufs.capstone.backend.place.api.response;

import com.hufs.capstone.backend.place.application.dto.RoomPlacePageResult;
import java.util.List;

public record RoomPlacePageResponse(
		List<RoomPlaceResponse> items,
		int page,
		int limit,
		long totalElements,
		int totalPages
) {

	public static RoomPlacePageResponse from(RoomPlacePageResult result) {
		return new RoomPlacePageResponse(
				result.items().stream()
						.map(RoomPlaceResponse::from)
						.toList(),
				result.page(),
				result.limit(),
				result.totalElements(),
				result.totalPages()
		);
	}
}
