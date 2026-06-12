package com.hufs.capstone.backend.place.application.dto;

import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.math.BigDecimal;

public record RoomPlaceMapItemResult(
		Long roomPlaceId,
		String name,
		BigDecimal latitude,
		BigDecimal longitude,
		String categoryCode,
		String tagCode
) {

	public static RoomPlaceMapItemResult from(RoomPlace roomPlace) {
		Place place = roomPlace.getPlace();
		return new RoomPlaceMapItemResult(
				roomPlace.getId(),
				place.getName(),
				place.getLatitude(),
				place.getLongitude(),
				place.getServiceCategory().getCode(),
				place.getServiceTag().getCode()
		);
	}
}
