package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.entity.LinkCandidate;
import com.hufs.capstone.backend.link.domain.entity.RoomLinkCandidateOverride;
import com.hufs.capstone.backend.link.domain.vo.PlaceCandidateSnapshot;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.math.BigDecimal;

public record LinkPlaceResult(
		Long candidateId,
		Long overrideId,
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
		String query,
		String evidenceText,
		String originalText,
		boolean alreadyInRoom,
		boolean selectable,
		boolean originalCandidate,
		boolean corrected,
		boolean editable,
		Long roomPlaceId,
		DisabledReason disabledReason
) {

	public static LinkPlaceResult fromCandidate(PlaceCandidateSnapshot candidate) {
		boolean hasKakaoPlaceId = candidate.kakaoPlaceId() != null && !candidate.kakaoPlaceId().isBlank();
		return fromCandidate(
				null,
				candidate,
				false,
				hasKakaoPlaceId,
				true,
				false,
				true,
				null,
				hasKakaoPlaceId ? null : DisabledReason.MISSING_KAKAO_PLACE_ID
		);
	}

	public static LinkPlaceResult fromCandidate(LinkCandidate candidate) {
		boolean hasKakaoPlaceId = candidate.getKakaoPlaceId() != null && !candidate.getKakaoPlaceId().isBlank();
		return fromCandidate(
				candidate.getId(),
				candidate.toSnapshot(),
				false,
				hasKakaoPlaceId,
				true,
				false,
				true,
				null,
				hasKakaoPlaceId ? null : DisabledReason.MISSING_KAKAO_PLACE_ID
		);
	}

	public static LinkPlaceResult fromOverride(LinkCandidate candidate, RoomLinkCandidateOverride override) {
		return new LinkPlaceResult(
				candidate.getId(),
				override.getId(),
				override.getKakaoPlaceId(),
				override.getName(),
				override.getCategoryName(),
				override.getCategoryGroupCode(),
				override.getCategoryGroupName(),
				override.getAddress(),
				override.getRoadAddress(),
				override.getLongitude(),
				override.getLatitude(),
				override.getPhone(),
				override.getPlaceUrl(),
				null,
				override.getQuery(),
				null,
				null,
				false,
				true,
				false,
				true,
				true,
				null,
				null
		);
	}

	public static LinkPlaceResult alreadyInRoom(PlaceCandidateSnapshot candidate, RoomPlace roomPlace) {
		return fromCandidate(
				null,
				candidate,
				true,
				false,
				true,
				false,
				true,
				roomPlace.getId(),
				DisabledReason.ALREADY_IN_ROOM
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
				candidate.categoryGroupName(),
				candidate.addressName(),
				candidate.roadAddressName(),
				candidate.longitude(),
				candidate.latitude(),
				candidate.phone(),
				candidate.placeUrl(),
				candidate.confidence(),
				candidate.query(),
				candidate.evidenceText(),
				candidate.originalText(),
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
			boolean alreadyInRoom,
			boolean selectable,
			boolean originalCandidate,
			boolean corrected,
			boolean editable,
			Long roomPlaceId,
			DisabledReason disabledReason
	) {
		return fromCandidate(
				candidateId,
				candidate,
				alreadyInRoom,
				selectable,
				originalCandidate,
				corrected,
				editable,
				roomPlaceId,
				disabledReason,
				null
		);
	}

	private static LinkPlaceResult fromCandidate(
			Long candidateId,
			PlaceCandidateSnapshot candidate,
			boolean alreadyInRoom,
			boolean selectable,
			boolean originalCandidate,
			boolean corrected,
			boolean editable,
			Long roomPlaceId,
			DisabledReason disabledReason,
			Long overrideId
	) {
		return new LinkPlaceResult(
				candidateId,
				overrideId,
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
				candidate.query(),
				candidate.evidenceText(),
				candidate.originalText(),
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
