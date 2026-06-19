package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.room.application.port.RoomPlaceCountPort;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
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

	@Override
	public Map<Long, Long> countPlacesByRoomIds(Collection<Long> roomIds) {
		if (roomIds == null || roomIds.isEmpty()) {
			return Map.of();
		}
		return roomPlaceRepository.countByRoomIds(roomIds).stream()
				.collect(Collectors.toMap(
						RoomPlaceRepository.RoomCountProjection::getRoomId,
						RoomPlaceRepository.RoomCountProjection::getCount
				));
	}
}
