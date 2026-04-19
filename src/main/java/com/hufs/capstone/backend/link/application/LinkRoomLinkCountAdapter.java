package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.room.application.port.RoomLinkCountPort;
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
}
