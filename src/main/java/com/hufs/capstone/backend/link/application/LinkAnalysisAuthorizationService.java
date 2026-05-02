package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkAnalysisAuthorizationService {

	private final RoomAccessService roomAccessService;
	private final RoomLinkRepository roomLinkRepository;

	public Room requireReadableRoom(Long userId, String roomId, Long linkId) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		if (!roomLinkRepository.existsByRoomAndLinkId(room, linkId)) {
			throw new BusinessException(ErrorCode.E403_FORBIDDEN, "Link is not shared in this room.");
		}
		return room;
	}

	public RoomLink requireRoomLink(Long userId, String roomId, Long linkId) {
		Room room = requireReadableRoom(userId, roomId, linkId);
		return roomLinkRepository.findByRoomAndLinkId(room, linkId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E403_FORBIDDEN, "Link is not shared in this room."));
	}
}
