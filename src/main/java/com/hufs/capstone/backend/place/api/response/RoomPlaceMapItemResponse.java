package com.hufs.capstone.backend.place.api.response;

import com.hufs.capstone.backend.place.application.dto.RoomPlaceMapItemResult;
import java.math.BigDecimal;

public record RoomPlaceMapItemResponse(
		Long roomPlaceId,
		String name,
		BigDecimal latitude,
		BigDecimal longitude,
		String categoryCode,
		String tagCode
) {

	public static RoomPlaceMapItemResponse from(RoomPlaceMapItemResult result) {
		return new RoomPlaceMapItemResponse(
				result.roomPlaceId(),
				result.name(),
				result.latitude(),
				result.longitude(),
				result.categoryCode(),
				result.tagCode()
		);
	}
}
