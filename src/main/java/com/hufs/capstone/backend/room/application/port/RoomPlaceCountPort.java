package com.hufs.capstone.backend.room.application.port;

import java.util.Collection;
import java.util.Map;

public interface RoomPlaceCountPort {

	long countPlacesInRoom(Long roomId);

	Map<Long, Long> countPlacesByRoomIds(Collection<Long> roomIds);
}
