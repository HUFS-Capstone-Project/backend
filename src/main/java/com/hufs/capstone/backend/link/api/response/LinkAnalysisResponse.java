package com.hufs.capstone.backend.link.api.response;

import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.application.dto.LinkPlaceResult;
import com.hufs.capstone.backend.link.application.dto.LinkPlaceResult.DisabledReason;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import java.math.BigDecimal;
import java.util.List;

public record LinkAnalysisResponse(
		Long linkId,
		LinkAnalysisStatus status,
		List<PlaceResponse> candidatePlaces,
		String errorCode,
		String errorMessage
) {

	public static LinkAnalysisResponse from(LinkAnalysisResult result) {
		return new LinkAnalysisResponse(
				result.linkId(),
				result.status(),
				result.candidatePlaces().stream()
						.map(PlaceResponse::from)
						.toList(),
				result.errorCode(),
				result.errorMessage()
		);
	}

	public record PlaceResponse(
			String kakaoPlaceId,
			String placeName,
			String categoryName,
			String categoryGroupCode,
			String categoryGroupName,
			String addressName,
			String roadAddressName,
			BigDecimal longitude,
			BigDecimal latitude,
			String phone,
			String placeUrl,
			String sourceKeyword,
			boolean alreadySaved,
			boolean selectable,
			Long roomPlaceId,
			DisabledReason disabledReason
	) {

		private static PlaceResponse from(LinkPlaceResult result) {
			return new PlaceResponse(
					result.kakaoPlaceId(),
					result.placeName(),
					result.categoryName(),
					result.categoryGroupCode(),
					result.categoryGroupName(),
					result.addressName(),
					result.roadAddressName(),
					result.longitude(),
					result.latitude(),
					result.phone(),
					result.placeUrl(),
					result.sourceKeyword(),
					result.alreadySaved(),
					result.selectable(),
					result.roomPlaceId(),
					result.disabledReason()
			);
		}
	}
}
