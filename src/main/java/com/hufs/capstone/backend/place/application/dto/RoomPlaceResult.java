package com.hufs.capstone.backend.place.application.dto;

import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceSourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RoomPlaceResult(
		Long roomPlaceId,
		Long placeId,
		String kakaoPlaceId,
		String name,
		String address,
		String roadAddress,
		BigDecimal latitude,
		BigDecimal longitude,
		String categoryName,
		String categoryGroupCode,
		String serviceCategoryCode,
		String serviceCategoryName,
		String serviceTagCode,
		String serviceTagName,
		String sidoCode,
		String sidoName,
		String sigunguCode,
		String sigunguName,
		String memo,
		List<RoomPlaceMemoResult> memos,
		RoomPlaceSourceType sourceType,
		Long sourceRoomLinkId,
		String sourceUrl,
		Long createdBy,
		Instant createdAt,
		String businessHoursStatus,
		BusinessHoursDisplayResult businessHours,
		Instant businessHoursFetchedAt,
		Instant businessHoursExpiresAt
) {

	public static RoomPlaceResult from(RoomPlace roomPlace) {
		return from(roomPlace, null, null, List.of(), null);
	}

	public static RoomPlaceResult from(RoomPlace roomPlace, BusinessHoursResult businessHours) {
		return from(roomPlace, businessHours, null, List.of(), null);
	}

	public static RoomPlaceResult from(RoomPlace roomPlace, BusinessHoursResult businessHours, String sourceUrl) {
		return from(roomPlace, businessHours, sourceUrl, List.of(), null);
	}

	public static RoomPlaceResult from(
			RoomPlace roomPlace,
			BusinessHoursResult businessHours,
			String sourceUrl,
			List<RoomPlaceMemoResult> memos,
			Long currentUserId
	) {
		Place place = roomPlace.getPlace();
		List<RoomPlaceMemoResult> normalizedMemos = memos == null ? List.of() : List.copyOf(memos);
		return new RoomPlaceResult(
				roomPlace.getId(),
				place.getId(),
				place.getKakaoPlaceId(),
				place.getName(),
				place.getAddress(),
				place.getRoadAddress(),
				place.getLatitude(),
				place.getLongitude(),
				place.getCategoryName(),
				place.getCategoryGroupCode(),
				place.getServiceCategory().getCode(),
				place.getServiceCategory().getName(),
				place.getServiceTag().getCode(),
				place.getServiceTag().getName(),
				roomPlace.getSidoCode(),
				roomPlace.getSidoName(),
				roomPlace.getSigunguCode(),
				roomPlace.getSigunguName(),
				currentUserMemo(normalizedMemos, currentUserId),
				normalizedMemos,
				roomPlace.getSourceType(),
				roomPlace.getSourceRoomLinkId(),
				sourceUrl,
				roomPlace.getCreatedByUserId(),
				roomPlace.getCreatedAt(),
				businessHours == null || businessHours.businessHoursStatus() == null
						? null
						: businessHours.businessHoursStatus().name(),
				businessHours == null ? null : businessHours.businessHours(),
				businessHours == null ? null : businessHours.businessHoursFetchedAt(),
				businessHours == null ? null : businessHours.businessHoursExpiresAt()
		);
	}

	private static String currentUserMemo(List<RoomPlaceMemoResult> memos, Long currentUserId) {
		if (currentUserId == null) {
			return null;
		}
		return memos.stream()
				.filter(memo -> currentUserId.equals(memo.userId()))
				.map(RoomPlaceMemoResult::memo)
				.findFirst()
				.orElse(null);
	}
}
