package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.room.application.port.RoomLinkCountPort;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LinkRoomLinkCountAdapter implements RoomLinkCountPort {

	private final RoomLinkRepository roomLinkRepository;

	@Override
	public long countLinksInRoom(Long roomId) {
		return roomLinkRepository.countByRoomId(roomId);
	}

	@Override
	public Map<Long, Long> countLinksByRoomIds(Collection<Long> roomIds) {
		if (roomIds == null || roomIds.isEmpty()) {
			return Map.of();
		}
		return roomLinkRepository.countByRoomIds(roomIds).stream()
				.collect(Collectors.toMap(
						RoomLinkRepository.RoomCountProjection::getRoomId,
						RoomLinkRepository.RoomCountProjection::getCount
				));
	}
}
