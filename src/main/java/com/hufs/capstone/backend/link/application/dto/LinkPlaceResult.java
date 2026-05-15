package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.entity.LinkCandidate;
import com.hufs.capstone.backend.link.domain.entity.RoomLinkCandidateOverride;
import com.hufs.capstone.backend.link.domain.vo.PlaceCandidateSnapshot;
import com.hufs.capstone.backend.place.application.dto.ResolvedPlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.math.BigDecimal;

public record LinkPlaceResult(
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

	public static LinkPlaceResult fromCandidate(PlaceCandidateSnapshot candidate, ResolvedPlaceCategory serviceCategory) {
		boolean hasKakaoPlaceId = candidate.kakaoPlaceId() != null && !candidate.kakaoPlaceId().isBlank();
		return fromCandidate(
				null,
				candidate,
				serviceCategory,
				false,
				hasKakaoPlaceId,
				true,
				false,
				true,
				null,
				hasKakaoPlaceId ? null : DisabledReason.MISSING_KAKAO_PLACE_ID
		);
	}

	public static LinkPlaceResult fromCandidate(LinkCandidate candidate, ResolvedPlaceCategory serviceCategory) {
		boolean hasKakaoPlaceId = candidate.getKakaoPlaceId() != null && !candidate.getKakaoPlaceId().isBlank();
		return fromCandidate(
				candidate.getId(),
				candidate.toSnapshot(),
				serviceCategory,
				false,
				hasKakaoPlaceId,
				true,
				false,
				true,
				null,
				hasKakaoPlaceId ? null : DisabledReason.MISSING_KAKAO_PLACE_ID
		);
	}

	public static LinkPlaceResult fromOverride(
			LinkCandidate candidate,
			RoomLinkCandidateOverride override,
			ResolvedPlaceCategory serviceCategory
	) {
		return new LinkPlaceResult(
				candidate.getId(),
				override.getId(),
				override.getKakaoPlaceId(),
				override.getName(),
				override.getCategoryName(),
				override.getCategoryGroupCode(),
				serviceCategory.code(),
				serviceCategory.name(),
				override.getAddress(),
				override.getRoadAddress(),
				override.getLongitude(),
				override.getLatitude(),
				override.getPhone(),
				override.getPlaceUrl(),
				false,
				true,
				false,
				true,
				true,
				null,
				null
		);
	}

	public static LinkPlaceResult alreadyInRoom(LinkPlaceResult candidate, RoomPlace roomPlace) {
		return new LinkPlaceResult(
				candidate.candidateId(),
				candidate.overrideId(),
				candidate.kakaoPlaceId(),
				candidate.placeName(),
				candidate.categoryName(),
				candidate.categoryGroupCode(),
				candidate.serviceCategoryCode(),
				candidate.serviceCategoryName(),
				candidate.addressName(),
				candidate.roadAddressName(),
				candidate.longitude(),
				candidate.latitude(),
				candidate.phone(),
				candidate.placeUrl(),
				true,
				false,
				candidate.originalCandidate(),
				candidate.corrected(),
				candidate.editable(),
				roomPlace.getId(),
				DisabledReason.ALREADY_IN_ROOM
		);
	}

	private static LinkPlaceResult fromCandidate(
			Long candidateId,
			PlaceCandidateSnapshot candidate,
			ResolvedPlaceCategory serviceCategory,
			boolean alreadyInRoom,
			boolean selectable,
			boolean originalCandidate,
			boolean corrected,
			boolean editable,
			Long roomPlaceId,
			DisabledReason disabledReason
	) {
		return new LinkPlaceResult(
				candidateId,
				null,
				candidate.kakaoPlaceId(),
				candidate.placeName(),
				candidate.categoryName(),
				candidate.categoryGroupCode(),
				serviceCategory.code(),
				serviceCategory.name(),
				candidate.addressName(),
				candidate.roadAddressName(),
				candidate.longitude(),
				candidate.latitude(),
				candidate.phone(),
				candidate.placeUrl(),
				alreadyInRoom,
				selectable,
				originalCandidate,
				corrected,
				editable,
				roomPlaceId,
				disabledReason
		);
	}

	public enum DisabledReason {
		ALREADY_IN_ROOM,
		MISSING_KAKAO_PLACE_ID
	}
}
