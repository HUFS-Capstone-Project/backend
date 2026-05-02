package com.hufs.capstone.backend.link.api.response;

import com.hufs.capstone.backend.link.application.dto.RoomPlaceSaveResult;
import java.util.List;

public record RoomPlaceSaveResponse(
		Long linkId,
		List<SavedPlaceResponse> places
) {

	public static RoomPlaceSaveResponse from(RoomPlaceSaveResult result) {
		return new RoomPlaceSaveResponse(
				result.linkId(),
				result.places().stream()
						.map(SavedPlaceResponse::from)
						.toList()
		);
	}

	public record SavedPlaceResponse(
			Long roomPlaceId,
			String kakaoPlaceId,
			String placeName,
			boolean created,
			boolean alreadySaved
	) {

		private static SavedPlaceResponse from(RoomPlaceSaveResult.SavedPlaceResult result) {
			return new SavedPlaceResponse(
					result.roomPlaceId(),
					result.kakaoPlaceId(),
					result.placeName(),
					result.created(),
					result.alreadySaved()
			);
		}
	}
}
