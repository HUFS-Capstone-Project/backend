package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.entity.RoomPlace;
import com.hufs.capstone.backend.link.domain.vo.PlaceCandidateSnapshot;
import java.math.BigDecimal;

public record LinkPlaceResult(
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
		BigDecimal confidence,
		String sourceKeyword,
		String sourceSentence,
		String rawCandidate,
		boolean alreadySaved,
		boolean selectable,
		Long roomPlaceId,
		DisabledReason disabledReason
) {

	public static LinkPlaceResult fromCandidate(PlaceCandidateSnapshot candidate) {
		boolean hasKakaoPlaceId = candidate.kakaoPlaceId() != null;
		return fromCandidate(
				candidate,
				false,
				hasKakaoPlaceId,
				null,
				hasKakaoPlaceId ? null : DisabledReason.MISSING_KAKAO_PLACE_ID
		);
	}

	public static LinkPlaceResult alreadySaved(PlaceCandidateSnapshot candidate, RoomPlace roomPlace) {
		return fromCandidate(candidate, true, false, roomPlace.getId(), DisabledReason.ALREADY_SAVED);
	}

	private static LinkPlaceResult fromCandidate(
			PlaceCandidateSnapshot candidate,
			boolean alreadySaved,
			boolean selectable,
			Long roomPlaceId,
			DisabledReason disabledReason
	) {
		return new LinkPlaceResult(
				candidate.kakaoPlaceId(),
				candidate.placeName(),
				candidate.categoryName(),
				candidate.categoryGroupCode(),
				candidate.categoryGroupName(),
				candidate.addressName(),
				candidate.roadAddressName(),
				candidate.longitude(),
				candidate.latitude(),
				candidate.phone(),
				candidate.placeUrl(),
				candidate.confidence(),
				candidate.sourceKeyword(),
				candidate.sourceSentence(),
				candidate.rawCandidate(),
				alreadySaved,
				selectable,
				roomPlaceId,
				disabledReason
		);
	}

	public enum DisabledReason {
		ALREADY_SAVED,
		MISSING_KAKAO_PLACE_ID
	}
}
