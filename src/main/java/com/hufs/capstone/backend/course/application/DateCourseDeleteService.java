package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.domain.entity.DateCourse;
import com.hufs.capstone.backend.course.domain.repository.DateCourseRepository;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DateCourseDeleteService {

	private final RoomAccessService roomAccessService;
	private final DateCourseRepository dateCourseRepository;

	@Transactional
	public void delete(String roomPublicId, String dateCourseId, Long userId) {
		// 1. 방 멤버 검증
		Room room = roomAccessService.requireMemberRoom(roomPublicId, userId);

		// 2. 코스 존재 확인 (soft delete 제외)
		DateCourse course = dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(
				dateCourseId, room.getId())
				.orElseThrow(() -> new BusinessException(ErrorCode.DATE_COURSE_NOT_FOUND));

		// 3. 저장된 코스만 삭제 대상 (미저장 후보는 사용자 가시 대상 아님)
		if (course.getSavedByUserId() == null) {
			throw new BusinessException(ErrorCode.DATE_COURSE_NOT_FOUND);
		}

		// 4. 권한 검증 — 생성자 또는 저장자만 삭제 가능
		boolean isOwner = userId.equals(course.getCreatedByUserId())
				|| userId.equals(course.getSavedByUserId());
		if (!isOwner) {
			throw new BusinessException(ErrorCode.DATE_COURSE_FORBIDDEN_DELETE);
		}

		// 5. Soft delete — DateCoursePlace 행은 보존하되 코스가 모든 조회에서 제외됨
		course.softDelete();
	}
}
