package com.hufs.capstone.backend.place.application.dto;

import com.hufs.capstone.backend.place.domain.enums.PlaceCandidateDisabledReason;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import java.math.BigDecimal;

public record PlaceCandidateResult(
		String kakaoPlaceId,
		String name,
		String address,
		String roadAddress,
		BigDecimal latitude,
		BigDecimal longitude,
		String categoryName,
		String categoryGroupCode,
		String categoryGroupName,
		String phone,
		String placeUrl,
		String source,
		boolean alreadyInRoom,
		Long roomPlaceId,
		boolean selectable,
		PlaceCandidateDisabledReason disabledReason
) {

	public static PlaceCandidateResult selectable(PlaceSnapshot snapshot) {
		return from(snapshot, false, null, true, null);
	}

	public static PlaceCandidateResult missingKakaoPlaceId(PlaceSnapshot snapshot) {
		return from(snapshot, false, null, false, PlaceCandidateDisabledReason.MISSING_KAKAO_PLACE_ID);
	}

	public static PlaceCandidateResult alreadyInRoom(PlaceSnapshot snapshot, Long roomPlaceId) {
		return from(snapshot, true, roomPlaceId, false, PlaceCandidateDisabledReason.ALREADY_IN_ROOM);
	}

	private static PlaceCandidateResult from(
			PlaceSnapshot snapshot,
			boolean alreadyInRoom,
			Long roomPlaceId,
			boolean selectable,
			PlaceCandidateDisabledReason disabledReason
	) {
		return new PlaceCandidateResult(
				snapshot.kakaoPlaceId(),
				snapshot.name(),
				snapshot.address(),
				snapshot.roadAddress(),
				snapshot.latitude(),
				snapshot.longitude(),
				snapshot.categoryName(),
				snapshot.categoryGroupCode(),
				snapshot.categoryGroupName(),
				snapshot.phone(),
				snapshot.placeUrl(),
				snapshot.source().name(),
				alreadyInRoom,
				roomPlaceId,
				selectable,
				disabledReason
		);
	}
}
