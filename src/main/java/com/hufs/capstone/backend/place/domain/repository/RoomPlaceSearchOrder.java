package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.QRoomPlace;
import com.querydsl.core.types.OrderSpecifier;

final class RoomPlaceSearchOrder {

	private static final QRoomPlace ROOM_PLACE = QRoomPlace.roomPlace;

	private RoomPlaceSearchOrder() {
	}

	static OrderSpecifier<?>[] newestFirst() {
		return new OrderSpecifier<?>[] {
				ROOM_PLACE.createdAt.desc(),
				ROOM_PLACE.id.desc()
		};
	}
}
