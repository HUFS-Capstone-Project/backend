package com.hufs.capstone.backend.link.api.response;

import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.application.dto.LinkPlaceResult;
import com.hufs.capstone.backend.link.application.dto.LinkPlaceResult.DisabledReason;
import com.hufs.capstone.backend.link.application.dto.LinkStatsResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.LinkSourceType;
import java.math.BigDecimal;
import java.util.List;

public record LinkAnalysisResponse(
		Long linkId,
		LinkAnalysisStatus status,
		String originalUrl,
		LinkSourceType linkSourceType,
		String contentText,
		LinkStatsResponse linkStats,
		List<PlaceResponse> candidatePlaces,
		String errorCode,
		String errorMessage,
		Boolean retryable,
		Integer cooldownSeconds
) {

	public static LinkAnalysisResponse from(LinkAnalysisResult result) {
		return new LinkAnalysisResponse(
				result.linkId(),
				result.status(),
				result.originalUrl(),
				result.linkSourceType(),
				result.contentText(),
				LinkStatsResponse.from(result.linkStats()),
				result.candidatePlaces().stream()
						.map(PlaceResponse::from)
						.toList(),
				result.errorCode(),
				result.errorMessage(),
				result.retryable(),
				result.cooldownSeconds()
		);
	}

	public record LinkStatsResponse(
			Long likeCount,
			Long commentCount,
			String postedAt
	) {

		private static LinkStatsResponse from(LinkStatsResult result) {
			if (result == null) {
				return new LinkStatsResponse(null, null, null);
			}
			return new LinkStatsResponse(result.likeCount(), result.commentCount(), result.postedAt());
		}
	}

	public record PlaceResponse(
			Long candidateId,
			Long overrideId,
			String kakaoPlaceId,
			String placeName,
			String categoryName,
			String categoryGroupCode,
			String serviceCategoryCode,
			String serviceCategoryName,
			String addressName,
			String roadAddressName,
			BigDecimal longitude,
			BigDecimal latitude,
			String phone,
			String placeUrl,
			boolean alreadyInRoom,
			boolean selectable,
			boolean originalCandidate,
			boolean corrected,
			boolean editable,
			Long roomPlaceId,
			DisabledReason disabledReason
	) {

		private static PlaceResponse from(LinkPlaceResult result) {
			return new PlaceResponse(
					result.candidateId(),
					result.overrideId(),
					result.kakaoPlaceId(),
					result.placeName(),
					result.categoryName(),
					result.categoryGroupCode(),
					result.serviceCategoryCode(),
					result.serviceCategoryName(),
					result.addressName(),
					result.roadAddressName(),
					result.longitude(),
					result.latitude(),
					result.phone(),
					result.placeUrl(),
					result.alreadyInRoom(),
					result.selectable(),
					result.originalCandidate(),
					result.corrected(),
					result.editable(),
					result.roomPlaceId(),
					result.disabledReason()
			);
		}
	}
}
