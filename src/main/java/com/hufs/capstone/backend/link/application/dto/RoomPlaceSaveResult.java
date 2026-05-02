package com.hufs.capstone.backend.link.application.dto;

import java.util.List;

public record RoomPlaceSaveResult(
		Long linkId,
		List<SavedPlaceResult> places
) {

	public record SavedPlaceResult(
			Long roomPlaceId,
			String kakaoPlaceId,
			String placeName,
			boolean created,
			boolean alreadySaved
	) {
	}
}
