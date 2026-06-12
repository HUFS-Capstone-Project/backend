package com.hufs.capstone.backend.place.api.response;

import com.hufs.capstone.backend.place.application.dto.RoomPlaceMapResult;
import java.util.List;

public record RoomPlaceMapResponse(
		List<RoomPlaceMapItemResponse> items,
		int limit,
		boolean truncated
) {

	public static RoomPlaceMapResponse from(RoomPlaceMapResult result) {
		return new RoomPlaceMapResponse(
				result.items().stream()
						.map(RoomPlaceMapItemResponse::from)
						.toList(),
				result.limit(),
				result.truncated()
		);
	}
}
