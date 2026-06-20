package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.domain.repository.DateCoursePlaceRepository;
import com.hufs.capstone.backend.course.domain.repository.DateCourseRepository;
import com.hufs.capstone.backend.room.application.port.RoomDataCleanupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
@RequiredArgsConstructor
public class DateCourseRoomCleanupAdapter implements RoomDataCleanupPort {

	private final DateCoursePlaceRepository dateCoursePlaceRepository;
	private final DateCourseRepository dateCourseRepository;

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void deleteAllByRoomId(Long roomId) {
		dateCoursePlaceRepository.deleteByRoomId(roomId);
		dateCourseRepository.deleteByRoomId(roomId);
	}
}
