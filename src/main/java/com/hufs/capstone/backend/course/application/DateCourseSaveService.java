package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.domain.DateCourseNamePolicy;
import com.hufs.capstone.backend.course.domain.entity.DateCourse;
import com.hufs.capstone.backend.course.domain.entity.DateCoursePlace;
import com.hufs.capstone.backend.course.domain.repository.DateCoursePlaceRepository;
import com.hufs.capstone.backend.course.domain.repository.DateCourseRepository;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.global.exception.FieldValidationException;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DateCourseSaveService {

	private final RoomAccessService roomAccessService;
	private final DateCourseRepository dateCourseRepository;
	private final DateCoursePlaceRepository dateCoursePlaceRepository;
	private final RoomPlaceRepository roomPlaceRepository;
	private final DateCourseDuplicatePolicy duplicatePolicy;

	@Transactional
	public void save(
			String roomPublicId,
			String dateCourseId,
			String courseName,
			List<Long> roomPlaceIds,
			Long userId
	) {
		Room room = roomAccessService.requireMemberRoom(roomPublicId, userId);
		String normalizedName = DateCourseNamePolicy.normalizeAndValidate(courseName);

		DateCourse course = dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(dateCourseId, room.getId())
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "데이트 코스를 찾을 수 없습니다."));

		if (course.getSavedByUserId() != null) {
			throw new BusinessException(ErrorCode.E409_CONFLICT, "이미 저장된 데이트 코스입니다.");
		}

		if (roomPlaceIds == null) {
			List<DateCoursePlace> places = dateCoursePlaceRepository.findWithRoomPlacesByCourseIdIn(List.of(course.getId()));
			if (duplicatePolicy.existsSavedCourseWithSamePlacesExcluding(room.getId(), course.getId(), places)) {
				throw new BusinessException(ErrorCode.E409_DUPLICATE_DATE_COURSE, "동일한 데이트 코스가 이미 저장되어 있습니다.");
			}
		} else {
			List<RoomPlace> orderedPlaces = validateAndLoadRoomPlaces(roomPlaceIds, room.getId());
			if (duplicatePolicy.existsSavedCourseWithSameRoomPlacesExcluding(room.getId(), course.getId(), orderedPlaces)) {
				throw new BusinessException(ErrorCode.E409_DUPLICATE_DATE_COURSE, "동일한 데이트 코스가 이미 저장되어 있습니다.");
			}
			replaceCoursePlaces(course, orderedPlaces);
			course.clearSkippedSlots();
		}

		int updated = dateCourseRepository.markAsSavedIfAbsent(
				course.getId(), userId, Instant.now(), normalizedName);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.E409_CONFLICT, "이미 저장된 데이트 코스입니다.");
		}
	}

	private List<RoomPlace> validateAndLoadRoomPlaces(List<Long> roomPlaceIds, Long roomId) {
		if (roomPlaceIds.isEmpty()) {
			throw new FieldValidationException("roomPlaceIds", "장소는 최소 1개 이상이어야 합니다.");
		}

		long distinctCount = roomPlaceIds.stream().distinct().count();
		if (distinctCount != roomPlaceIds.size()) {
			throw new FieldValidationException("roomPlaceIds", "중복된 장소 ID가 포함되어 있습니다.");
		}

		List<RoomPlace> foundRoomPlaces = roomPlaceRepository.findAllByIdInAndRoomId(roomPlaceIds, roomId);
		if (foundRoomPlaces.size() != roomPlaceIds.size()) {
			throw new FieldValidationException("roomPlaceIds", "이 방에 저장된 장소만 추가할 수 있습니다.");
		}

		Map<Long, RoomPlace> roomPlaceById = foundRoomPlaces.stream()
				.collect(Collectors.toMap(RoomPlace::getId, roomPlace -> roomPlace));
		return roomPlaceIds.stream()
				.map(roomPlaceById::get)
				.toList();
	}

	private void replaceCoursePlaces(DateCourse course, List<RoomPlace> orderedPlaces) {
		dateCoursePlaceRepository.deleteByDateCourseId(course.getId());

		List<DateCoursePlace> newPlaces = new ArrayList<>();
		for (int i = 0; i < orderedPlaces.size(); i++) {
			newPlaces.add(DateCoursePlace.create(course, orderedPlaces.get(i), i));
		}
		dateCoursePlaceRepository.saveAll(newPlaces);
	}
}
