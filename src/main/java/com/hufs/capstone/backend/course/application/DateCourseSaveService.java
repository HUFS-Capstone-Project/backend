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
import org.springframework.dao.DataIntegrityViolationException;
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
				.orElseThrow(() -> new BusinessException(ErrorCode.DATE_COURSE_NOT_FOUND));

		if (course.getSavedByUserId() != null) {
			throw new BusinessException(ErrorCode.DATE_COURSE_ALREADY_SAVED);
		}
		Long courseDbId = course.getId();

		if (roomPlaceIds == null) {
			List<DateCoursePlace> places = dateCoursePlaceRepository.findWithRoomPlacesByCourseIdForUpdate(courseDbId);
			if (places.isEmpty()) {
				throw new BusinessException(ErrorCode.DATE_COURSE_NO_PLACES);
			}
			List<Long> originalRoomPlaceIds = places.stream()
					.map(DateCoursePlace::getRoomPlace)
					.map(RoomPlace::getId)
					.toList();
			validateAndLoadRoomPlaces(originalRoomPlaceIds, room.getId());
			if (duplicatePolicy.existsSavedCourseWithSamePlacesExcluding(room.getId(), courseDbId, places)) {
				throw new BusinessException(ErrorCode.E409_DUPLICATE_DATE_COURSE);
			}
		} else {
			List<RoomPlace> orderedPlaces = validateAndLoadRoomPlaces(roomPlaceIds, room.getId());
			if (duplicatePolicy.existsSavedCourseWithSameRoomPlacesExcluding(room.getId(), courseDbId, orderedPlaces)) {
				throw new BusinessException(ErrorCode.E409_DUPLICATE_DATE_COURSE);
			}
			course.clearSkippedSlots();
			replaceCoursePlaces(course, orderedPlaces);
		}

		int updated = dateCourseRepository.markAsSavedIfAbsent(
				courseDbId, userId, Instant.now(), normalizedName);
		if (updated == 0) {
			if (!dateCourseRepository.existsByIdAndDeletedAtIsNull(courseDbId)) {
				throw new BusinessException(ErrorCode.DATE_COURSE_NOT_FOUND);
			}
			throw new BusinessException(ErrorCode.DATE_COURSE_ALREADY_SAVED);
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

		List<Long> lockOrderedRoomPlaceIds = roomPlaceIds.stream()
				.sorted()
				.toList();
		List<RoomPlace> foundRoomPlaces = roomPlaceRepository.findAllByIdInAndRoomIdForUpdate(
				lockOrderedRoomPlaceIds,
				roomId
		);
		if (foundRoomPlaces.size() != roomPlaceIds.size()) {
			throw invalidRoomPlaceIds();
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
		try {
			dateCoursePlaceRepository.saveAllAndFlush(newPlaces);
		} catch (DataIntegrityViolationException ex) {
			throw invalidRoomPlaceIds();
		}
	}

	private static FieldValidationException invalidRoomPlaceIds() {
		return new FieldValidationException("roomPlaceIds", "이 방에 저장된 장소만 추가할 수 있습니다.");
	}
}
