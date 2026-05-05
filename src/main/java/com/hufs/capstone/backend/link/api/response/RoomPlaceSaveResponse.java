package com.hufs.capstone.backend.link.api.response;

import com.hufs.capstone.backend.place.application.dto.RoomPlaceSaveResult;
import java.util.List;

public record RoomPlaceSaveResponse(
		Long analysisRequestId,
		Long linkId,
		List<SavedPlaceResponse> places
) {

	public static RoomPlaceSaveResponse from(RoomPlaceSaveResult result) {
		return new RoomPlaceSaveResponse(
				result.analysisRequestId(),
				result.linkId(),
				result.places().stream()
						.map(SavedPlaceResponse::from)
						.toList()
		);
	}

	public record SavedPlaceResponse(
			Long roomPlaceId,
			Long placeId,
			String kakaoPlaceId,
			String name,
			boolean created,
			boolean alreadyInRoom
	) {

		private static SavedPlaceResponse from(RoomPlaceSaveResult.SavedRoomPlaceResult result) {
			return new SavedPlaceResponse(
					result.roomPlaceId(),
					result.placeId(),
					result.kakaoPlaceId(),
					result.name(),
					result.created(),
					result.alreadyInRoom()
			);
		}
	}
}
