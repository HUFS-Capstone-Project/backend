package com.hufs.capstone.backend.place.application.dto;

import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.room.domain.entity.Room;

public record MyRoomPlaceResult(
		RoomPlaceResult place,
		RoomResult room
) {

	public static MyRoomPlaceResult from(RoomPlace roomPlace, BusinessHoursResult businessHours) {
		return new MyRoomPlaceResult(
				RoomPlaceResult.from(roomPlace, businessHours),
				RoomResult.from(roomPlace.getRoom())
		);
	}

	public record RoomResult(
			String roomId,
			String roomName
	) {

		public static RoomResult from(Room room) {
			return new RoomResult(room.getPublicId(), room.getName());
		}
	}
}
