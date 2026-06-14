package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.course.domain.repository.DateCoursePlaceRepository;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.entity.RoomPlaceMemo;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceMemoRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceOriginRepository;
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
	private final RoomPlaceMemoRepository roomPlaceMemoRepository;
	private final RoomPlaceOriginRepository roomPlaceOriginRepository;
	private final DateCoursePlaceRepository dateCoursePlaceRepository;

	@Transactional
	public void updateMemo(Long userId, String roomId, Long roomPlaceId, String memo) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		RoomPlace roomPlace = getRoomPlaceOrThrow(room.getId(), roomPlaceId);
		String normalizedMemo = trimToNull(memo);
		if (normalizedMemo == null) {
			roomPlaceMemoRepository.deleteByRoomPlaceIdAndUserId(roomPlace.getId(), userId);
			return;
		}
		roomPlaceMemoRepository.findByRoomPlaceIdAndUserId(roomPlace.getId(), userId)
				.ifPresentOrElse(
						roomPlaceMemo -> roomPlaceMemo.update(normalizedMemo),
						() -> roomPlaceMemoRepository.save(RoomPlaceMemo.create(roomPlace, userId, normalizedMemo))
			);
	}

	@Transactional
	public void deleteRoomPlace(Long userId, String roomId, Long roomPlaceId) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		RoomPlace roomPlace = getRoomPlaceOrThrow(room.getId(), roomPlaceId);
		if (dateCoursePlaceRepository.existsByRoomPlaceIdInSavedDateCourse(roomPlace.getId())) {
			throw new BusinessException(
					ErrorCode.ROOM_PLACE_USED_IN_DATE_COURSE,
					"저장된 데이트코스에 포함된 장소는 삭제할 수 없습니다."
			);
		}
		dateCoursePlaceRepository.deleteByRoomPlaceId(roomPlace.getId());
		roomPlaceMemoRepository.deleteByRoomPlaceId(roomPlace.getId());
		roomPlaceOriginRepository.deleteByRoomPlaceId(roomPlace.getId());
		roomPlaceRepository.delete(roomPlace);
	}

	private RoomPlace getRoomPlaceOrThrow(Long roomId, Long roomPlaceId) {
		return roomPlaceRepository.findByIdAndRoomId(roomPlaceId, roomId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "방 장소를 찾을 수 없습니다."));
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
