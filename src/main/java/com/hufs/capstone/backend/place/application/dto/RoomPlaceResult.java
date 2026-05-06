package com.hufs.capstone.backend.place.application.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceSourceType;
import java.math.BigDecimal;
import java.time.Instant;

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
		String categoryGroupName,
		String serviceCategoryCode,
		String serviceCategoryName,
		String serviceTagCode,
		String serviceTagName,
		String sidoCode,
		String sidoName,
		String sigunguCode,
		String sigunguName,
		String memo,
		RoomPlaceSourceType sourceType,
		Long sourceRoomLinkId,
		String sourceUrl,
		Long createdBy,
		Instant createdAt,
		JsonNode businessHours,
		String businessHoursRaw,
		String businessHoursStatus,
		Instant businessHoursFetchedAt,
		Instant businessHoursExpiresAt,
		String businessHoursSource
) {

	public static RoomPlaceResult from(RoomPlace roomPlace) {
		return from(roomPlace, null);
	}

	public static RoomPlaceResult from(RoomPlace roomPlace, BusinessHoursResult businessHours) {
		return from(roomPlace, businessHours, null);
	}

	public static RoomPlaceResult from(RoomPlace roomPlace, BusinessHoursResult businessHours, String sourceUrl) {
		Place place = roomPlace.getPlace();
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
				place.getCategoryGroupName(),
				place.getServiceCategory().getCode(),
				place.getServiceCategory().getName(),
				place.getServiceTag().getCode(),
				place.getServiceTag().getName(),
				roomPlace.getSidoCode(),
				roomPlace.getSidoName(),
				roomPlace.getSigunguCode(),
				roomPlace.getSigunguName(),
				roomPlace.getMemo(),
				roomPlace.getSourceType(),
				roomPlace.getSourceRoomLinkId(),
				sourceUrl,
				roomPlace.getCreatedByUserId(),
				roomPlace.getCreatedAt(),
				businessHours == null ? null : businessHours.businessHours(),
				businessHours == null ? null : businessHours.businessHoursRaw(),
				businessHours == null || businessHours.businessHoursStatus() == null
						? null
						: businessHours.businessHoursStatus().name(),
				businessHours == null ? null : businessHours.businessHoursFetchedAt(),
				businessHours == null ? null : businessHours.businessHoursExpiresAt(),
				businessHours == null ? null : businessHours.businessHoursSource()
		);
	}
}
