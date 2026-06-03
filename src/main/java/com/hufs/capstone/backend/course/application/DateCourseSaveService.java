package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.domain.entity.DateCourse;
import com.hufs.capstone.backend.course.domain.repository.DateCourseRepository;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DateCourseSaveService {

	private final RoomAccessService roomAccessService;
	private final DateCourseRepository dateCourseRepository;

	@Transactional
	public void save(String roomPublicId, String coursePublicId, Long userId) {
		Room room = roomAccessService.requireMemberRoom(roomPublicId, userId);

		DateCourse course = dateCourseRepository.findByPublicIdAndRoomId(coursePublicId, room.getId())
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "코스를 찾을 수 없습니다."));

		int updated = dateCourseRepository.markAsSavedIfAbsent(course.getId(), userId, Instant.now());
		if (updated == 0) {
			throw new BusinessException(ErrorCode.E409_CONFLICT, "이미 저장된 코스입니다.");
		}
	}
}
