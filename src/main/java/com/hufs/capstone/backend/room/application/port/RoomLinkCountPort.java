package com.hufs.capstone.backend.room.application.port;

import java.util.Collection;
import java.util.Map;

public interface RoomLinkCountPort {

	long countLinksInRoom(Long roomId);

	Map<Long, Long> countLinksByRoomIds(Collection<Long> roomIds);
}
