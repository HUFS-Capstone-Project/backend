package com.hufs.capstone.backend.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.course.domain.entity.DateCourse;
import com.hufs.capstone.backend.course.domain.repository.DateCourseRepository;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DateCourseDeleteServiceTest {

	private static final String ROOM_PUBLIC_ID = "room-public-id";
	private static final String COURSE_UUID = "course-uuid-1234";
	private static final Long USER_ID = 100L;
	private static final Long ROOM_ID = 10L;

	@Mock
	private RoomAccessService roomAccessService;
	@Mock
	private DateCourseRepository dateCourseRepository;

	@InjectMocks
	private DateCourseDeleteService deleteService;

	@Test
	void deleteCallsSoftDelete() {
		Room room = mockRoom();
		DateCourse course = mockSavedCourse(USER_ID, USER_ID);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(COURSE_UUID, ROOM_ID))
				.thenReturn(Optional.of(course));

		deleteService.delete(ROOM_PUBLIC_ID, COURSE_UUID, USER_ID);

		verify(course).softDelete();
	}

	@Test
	void deleteAllowsSaverToDelete() {
		Room room = mockRoom();
		DateCourse course = mockSavedCourse(888L, USER_ID); // 생성자 다름, 저장자=요청자

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(COURSE_UUID, ROOM_ID))
				.thenReturn(Optional.of(course));

		deleteService.delete(ROOM_PUBLIC_ID, COURSE_UUID, USER_ID);

		verify(course).softDelete();
	}

	@Test
	void deleteThrows403WhenUnauthorized() {
		Room room = mockRoom();
		DateCourse course = mockSavedCourse(888L, 999L); // 생성자도 저장자도 요청자 아님

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(COURSE_UUID, ROOM_ID))
				.thenReturn(Optional.of(course));

		assertThatThrownBy(() -> deleteService.delete(ROOM_PUBLIC_ID, COURSE_UUID, USER_ID))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.E403_FORBIDDEN));
	}

	@Test
	void deleteThrows404WhenCourseNotFound() {
		Room room = mockRoom();
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(COURSE_UUID, ROOM_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> deleteService.delete(ROOM_PUBLIC_ID, COURSE_UUID, USER_ID))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.E404_NOT_FOUND));
	}

	@Test
	void deleteThrows404WhenCourseNotSaved() {
		Room room = mockRoom();
		DateCourse course = mock(DateCourse.class);
		when(course.getSavedByUserId()).thenReturn(null);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(COURSE_UUID, ROOM_ID))
				.thenReturn(Optional.of(course));

		assertThatThrownBy(() -> deleteService.delete(ROOM_PUBLIC_ID, COURSE_UUID, USER_ID))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.E404_NOT_FOUND));
	}

	private Room mockRoom() {
		Room room = mock(Room.class);
		when(room.getId()).thenReturn(ROOM_ID);
		return room;
	}

	private DateCourse mockSavedCourse(Long createdByUserId, Long savedByUserId) {
		DateCourse course = mock(DateCourse.class);
		when(course.getSavedByUserId()).thenReturn(savedByUserId);
		when(course.getCreatedByUserId()).thenReturn(createdByUserId);
		return course;
	}
}
