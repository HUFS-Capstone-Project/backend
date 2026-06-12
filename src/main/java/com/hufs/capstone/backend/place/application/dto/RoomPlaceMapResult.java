package com.hufs.capstone.backend.place.application.dto;

import java.util.List;

public record RoomPlaceMapResult(
		List<RoomPlaceMapItemResult> items,
		int limit,
		boolean truncated
) {
}
