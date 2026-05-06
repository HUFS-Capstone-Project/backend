package com.hufs.capstone.backend.place.application.dto;

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
		Long createdBy,
		Instant createdAt
) {

	public static RoomPlaceResult from(RoomPlace roomPlace) {
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
				roomPlace.getCreatedByUserId(),
				roomPlace.getCreatedAt()
		);
	}
}
