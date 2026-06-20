package com.hufs.capstone.backend.room.application.port;

public interface RoomDataCleanupPort {

	void deleteAllByRoomId(Long roomId);
}
