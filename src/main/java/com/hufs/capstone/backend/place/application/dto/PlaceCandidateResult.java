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
		String serviceCategoryCode,
		String serviceCategoryName,
		String phone,
		String placeUrl,
		String source,
		boolean alreadyInRoom,
		Long roomPlaceId,
		boolean selectable,
		PlaceCandidateDisabledReason disabledReason
) {

	public static PlaceCandidateResult selectable(PlaceSnapshot snapshot, ResolvedPlaceCategory serviceCategory) {
		return from(snapshot, serviceCategory, false, null, true, null);
	}

	public static PlaceCandidateResult missingKakaoPlaceId(PlaceSnapshot snapshot, ResolvedPlaceCategory serviceCategory) {
		return from(snapshot, serviceCategory, false, null, false, PlaceCandidateDisabledReason.MISSING_KAKAO_PLACE_ID);
	}

	public static PlaceCandidateResult alreadyInRoom(
			PlaceSnapshot snapshot,
			ResolvedPlaceCategory serviceCategory,
			Long roomPlaceId
	) {
		return from(snapshot, serviceCategory, true, roomPlaceId, false, PlaceCandidateDisabledReason.ALREADY_IN_ROOM);
	}

	private static PlaceCandidateResult from(
			PlaceSnapshot snapshot,
			ResolvedPlaceCategory serviceCategory,
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
				serviceCategory.code(),
				serviceCategory.name(),
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
