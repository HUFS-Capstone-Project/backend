package com.hufs.capstone.backend.place.api.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceResult;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceSourceType;
import java.math.BigDecimal;
import java.time.Instant;

public record RoomPlaceResponse(
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

	public static RoomPlaceResponse from(RoomPlaceResult result) {
		return new RoomPlaceResponse(
				result.roomPlaceId(),
				result.placeId(),
				result.kakaoPlaceId(),
				result.name(),
				result.address(),
				result.roadAddress(),
				result.latitude(),
				result.longitude(),
				result.categoryName(),
				result.categoryGroupCode(),
				result.categoryGroupName(),
				result.serviceCategoryCode(),
				result.serviceCategoryName(),
				result.serviceTagCode(),
				result.serviceTagName(),
				result.sidoCode(),
				result.sidoName(),
				result.sigunguCode(),
				result.sigunguName(),
				result.memo(),
				result.sourceType(),
				result.sourceRoomLinkId(),
				result.sourceUrl(),
				result.createdBy(),
				result.createdAt(),
				result.businessHours(),
				result.businessHoursRaw(),
				result.businessHoursStatus(),
				result.businessHoursFetchedAt(),
				result.businessHoursExpiresAt(),
				result.businessHoursSource()
		);
	}
}
