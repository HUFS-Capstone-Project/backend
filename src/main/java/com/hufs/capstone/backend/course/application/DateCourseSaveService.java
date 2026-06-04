package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.domain.DateCourseNamePolicy;
import com.hufs.capstone.backend.course.domain.entity.DateCourse;
import com.hufs.capstone.backend.course.domain.entity.DateCoursePlace;
import com.hufs.capstone.backend.course.domain.repository.DateCoursePlaceRepository;
import com.hufs.capstone.backend.course.domain.repository.DateCourseRepository;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DateCourseSaveService {

	private final RoomAccessService roomAccessService;
	private final DateCourseRepository dateCourseRepository;
	private final DateCoursePlaceRepository dateCoursePlaceRepository;
	private final DateCourseDuplicatePolicy duplicatePolicy;

	@Transactional
	public void save(String roomPublicId, String dateCourseId, String courseName, Long userId) {
		Room room = roomAccessService.requireMemberRoom(roomPublicId, userId);
		String normalizedName = DateCourseNamePolicy.normalizeAndValidate(courseName);

		DateCourse course = dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(dateCourseId, room.getId())
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "데이트 코스를 찾을 수 없습니다."));

		List<DateCoursePlace> places = dateCoursePlaceRepository.findWithRoomPlacesByCourseIdIn(List.of(course.getId()));
		if (duplicatePolicy.existsSavedCourseWithSamePlacesExcluding(room.getId(), course.getId(), places)) {
			throw new BusinessException(ErrorCode.E409_DUPLICATE_DATE_COURSE, "동일한 데이트 코스가 이미 저장되어 있습니다.");
		}

		int updated = dateCourseRepository.markAsSavedIfAbsent(
				course.getId(), userId, Instant.now(), normalizedName);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.E409_CONFLICT, "이미 저장된 데이트 코스입니다.");
		}
	}
}
