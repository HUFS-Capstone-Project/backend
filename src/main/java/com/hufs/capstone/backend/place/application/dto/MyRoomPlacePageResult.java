package com.hufs.capstone.backend.place.application.dto;

import java.util.List;

public record MyRoomPlacePageResult(
		List<MyRoomPlaceResult> items,
		int page,
		int limit,
		long totalElements,
		int totalPages
) {
}
