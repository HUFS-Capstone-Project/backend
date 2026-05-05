package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.room.application.port.RoomPlaceCountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LinkRoomPlaceCountAdapter implements RoomPlaceCountPort {

	private final RoomPlaceRepository roomPlaceRepository;

	@Override
	public long countPlacesInRoom(Long roomId) {
		return roomPlaceRepository.countByRoomId(roomId);
	}
}
