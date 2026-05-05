package com.hufs.capstone.backend.place.application.dto;

import java.util.List;

public record RoomPlacePageResult(
		List<RoomPlaceResult> items,
		int page,
		int limit,
		long totalElements,
		int totalPages
) {
}
