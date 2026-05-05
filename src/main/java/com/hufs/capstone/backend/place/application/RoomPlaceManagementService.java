package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceSourceRepository;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomPlaceManagementService {

	private final RoomAccessService roomAccessService;
	private final RoomPlaceRepository roomPlaceRepository;
	private final RoomPlaceSourceRepository roomPlaceSourceRepository;

	@Transactional
	public void updateMemo(Long userId, String roomId, Long roomPlaceId, String memo) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		RoomPlace roomPlace = getRoomPlaceOrThrow(room.getId(), roomPlaceId);
		roomPlace.updateMemo(memo);
	}

	@Transactional
	public void deleteRoomPlace(Long userId, String roomId, Long roomPlaceId) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		RoomPlace roomPlace = getRoomPlaceOrThrow(room.getId(), roomPlaceId);
		roomPlaceSourceRepository.deleteByRoomPlaceId(roomPlace.getId());
		roomPlaceRepository.delete(roomPlace);
	}

	private RoomPlace getRoomPlaceOrThrow(Long roomId, Long roomPlaceId) {
		return roomPlaceRepository.findByIdAndRoomId(roomPlaceId, roomId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "Room place not found."));
	}
}
