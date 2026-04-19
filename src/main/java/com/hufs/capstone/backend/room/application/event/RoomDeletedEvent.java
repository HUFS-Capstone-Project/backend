package com.hufs.capstone.backend.room.application.event;

public record RoomDeletedEvent(
		Long roomId,
		String roomPublicId
) {
}
