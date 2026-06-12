package com.hufs.capstone.backend.place.application.dto;

import java.time.Instant;

public record RoomPlaceCursor(
		Instant createdAt,
		Long roomPlaceId
) {

	public RoomPlaceCursor {
		if (createdAt == null || roomPlaceId == null) {
			throw new IllegalArgumentException("Room place cursor values are required.");
		}
	}
}
