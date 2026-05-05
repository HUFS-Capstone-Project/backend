package com.hufs.capstone.backend.place.application.dto;

import java.util.List;

public record RoomPlaceSaveResult(
		Long analysisRequestId,
		Long linkId,
		List<SavedRoomPlaceResult> places
) {

	public RoomPlaceSaveResult(Long linkId, List<SavedRoomPlaceResult> places) {
		this(null, linkId, places);
	}

	public record SavedRoomPlaceResult(
			Long roomPlaceId,
			Long placeId,
			String kakaoPlaceId,
			String name,
			boolean created,
			boolean alreadyInRoom
	) {
	}
}
