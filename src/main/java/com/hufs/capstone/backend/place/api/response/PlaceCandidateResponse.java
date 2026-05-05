package com.hufs.capstone.backend.place.api.response;

import com.hufs.capstone.backend.place.application.dto.PlaceCandidateResult;
import com.hufs.capstone.backend.place.domain.enums.PlaceCandidateDisabledReason;
import java.math.BigDecimal;

public record PlaceCandidateResponse(
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

	public static PlaceCandidateResponse from(PlaceCandidateResult result) {
		return new PlaceCandidateResponse(
				result.kakaoPlaceId(),
				result.name(),
				result.address(),
				result.roadAddress(),
				result.latitude(),
				result.longitude(),
				result.categoryName(),
				result.categoryGroupCode(),
				result.categoryGroupName(),
				result.phone(),
				result.placeUrl(),
				result.source(),
				result.alreadyInRoom(),
				result.roomPlaceId(),
				result.selectable(),
				result.disabledReason()
		);
	}
}
