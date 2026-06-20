package com.hufs.capstone.backend.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class DateCourseSaveServiceTest {

	private static final String ROOM_PUBLIC_ID = "room-public-id";
	private static final String DATE_COURSE_ID = "course-id";
	private static final Long USER_ID = 100L;
	private static final Long ROOM_ID = 10L;
	private static final Long COURSE_DB_ID = 1L;

	@Mock
	private RoomAccessService roomAccessService;
	@Mock
	private DateCourseRepository dateCourseRepository;
	@Mock
	private DateCoursePlaceRepository dateCoursePlaceRepository;
	@Mock
	private RoomPlaceRepository roomPlaceRepository;
	@Mock
	private DateCourseDuplicatePolicy duplicatePolicy;

	@InjectMocks
	private DateCourseSaveService saveService;

	@Test
	void saveWithRoomPlaceIdsReplacesPlacesBeforeSaving() {
		Room room = mockRoom();
		DateCourse course = mockUnsavedCourse();
		when(course.getId()).thenReturn(COURSE_DB_ID);
		RoomPlace rp2 = mockRoomPlace(2L);
		RoomPlace rp1 = mockRoomPlace(1L);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(DATE_COURSE_ID, ROOM_ID))
				.thenReturn(Optional.of(course));
		when(roomPlaceRepository.findAllByIdInAndRoomIdForUpdate(List.of(1L, 2L), ROOM_ID))
				.thenReturn(List.of(rp1, rp2));
		when(duplicatePolicy.existsSavedCourseWithSameRoomPlacesExcluding(anyLong(), anyLong(), any()))
				.thenReturn(false);
		when(dateCourseRepository.markAsSavedIfAbsent(anyLong(), anyLong(), any(), any()))
				.thenReturn(1);

		saveService.save(ROOM_PUBLIC_ID, DATE_COURSE_ID, "수정한 코스", List.of(2L, 1L), USER_ID);

		InOrder inOrder = inOrder(course, dateCoursePlaceRepository);
		inOrder.verify(course).clearSkippedSlots();
		inOrder.verify(dateCoursePlaceRepository).deleteByDateCourseId(COURSE_DB_ID);
		verify(dateCoursePlaceRepository).saveAllAndFlush(any());
		verify(dateCourseRepository).markAsSavedIfAbsent(eq(COURSE_DB_ID), eq(USER_ID), any(), any());
	}

	@Test
	void saveWithRoomPlaceIdsRejectsDuplicateIds() {
		Room room = mockRoom();
		DateCourse course = mockUnsavedCourse();

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(DATE_COURSE_ID, ROOM_ID))
				.thenReturn(Optional.of(course));

		assertThatThrownBy(() -> saveService.save(
				ROOM_PUBLIC_ID, DATE_COURSE_ID, "코스", List.of(1L, 1L), USER_ID))
				.isInstanceOf(FieldValidationException.class);

		verify(roomPlaceRepository, never()).findAllByIdInAndRoomIdForUpdate(any(), anyLong());
		verify(dateCourseRepository, never()).markAsSavedIfAbsent(anyLong(), anyLong(), any(), any());
	}

	@Test
	void saveWithRoomPlaceIdsRejectsPlacesOutsideRoom() {
		Room room = mockRoom();
		DateCourse course = mockUnsavedCourse();
		RoomPlace validRoomPlace = mock(RoomPlace.class);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(DATE_COURSE_ID, ROOM_ID))
				.thenReturn(Optional.of(course));
		when(roomPlaceRepository.findAllByIdInAndRoomIdForUpdate(List.of(1L, 999L), ROOM_ID))
				.thenReturn(List.of(validRoomPlace));

		assertThatThrownBy(() -> saveService.save(
				ROOM_PUBLIC_ID, DATE_COURSE_ID, "코스", List.of(1L, 999L), USER_ID))
				.isInstanceOf(FieldValidationException.class);

		verify(dateCourseRepository, never()).markAsSavedIfAbsent(anyLong(), anyLong(), any(), any());
	}

	@Test
	void saveWithRoomPlaceIdsRejectsWhenRoomPlaceIsDeletedConcurrently() {
		Room room = mockRoom();
		DateCourse course = mockUnsavedCourse();
		when(course.getId()).thenReturn(COURSE_DB_ID);
		RoomPlace roomPlace = mockRoomPlace(1L);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(DATE_COURSE_ID, ROOM_ID))
				.thenReturn(Optional.of(course));
		when(roomPlaceRepository.findAllByIdInAndRoomIdForUpdate(List.of(1L), ROOM_ID))
				.thenReturn(List.of(roomPlace));
		when(duplicatePolicy.existsSavedCourseWithSameRoomPlacesExcluding(anyLong(), anyLong(), any()))
				.thenReturn(false);
		when(dateCoursePlaceRepository.saveAllAndFlush(any()))
				.thenThrow(new DataIntegrityViolationException("fk"));

		assertThatThrownBy(() -> saveService.save(
				ROOM_PUBLIC_ID, DATE_COURSE_ID, "코스", List.of(1L), USER_ID))
				.isInstanceOf(FieldValidationException.class);

		verify(dateCourseRepository, never()).markAsSavedIfAbsent(anyLong(), anyLong(), any(), any());
	}

	@Test
	void saveWithoutRoomPlaceIdsRejectsWhenOriginalPlacesAreGone() {
		Room room = mockRoom();
		DateCourse course = mockUnsavedCourse();
		when(course.getId()).thenReturn(COURSE_DB_ID);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(DATE_COURSE_ID, ROOM_ID))
				.thenReturn(Optional.of(course));
		when(dateCoursePlaceRepository.findWithRoomPlacesByCourseIdForUpdate(COURSE_DB_ID))
				.thenReturn(List.of());

		assertThatThrownBy(() -> saveService.save(
				ROOM_PUBLIC_ID, DATE_COURSE_ID, "코스", null, USER_ID))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.DATE_COURSE_NO_PLACES));

		verify(dateCourseRepository, never()).markAsSavedIfAbsent(anyLong(), anyLong(), any(), any());
	}

	@Test
	void saveWithoutRoomPlaceIdsUsesLockedOriginalPlaces() {
		Room room = mockRoom();
		DateCourse course = mockUnsavedCourse();
		when(course.getId()).thenReturn(COURSE_DB_ID);
		RoomPlace roomPlace = mockRoomPlace(1L);
		DateCoursePlace coursePlace = mock(DateCoursePlace.class);
		when(coursePlace.getRoomPlace()).thenReturn(roomPlace);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(DATE_COURSE_ID, ROOM_ID))
				.thenReturn(Optional.of(course));
		when(dateCoursePlaceRepository.findWithRoomPlacesByCourseIdForUpdate(COURSE_DB_ID))
				.thenReturn(List.of(coursePlace));
		when(roomPlaceRepository.findAllByIdInAndRoomIdForUpdate(List.of(1L), ROOM_ID))
				.thenReturn(List.of(roomPlace));
		when(duplicatePolicy.existsSavedCourseWithSamePlacesExcluding(ROOM_ID, COURSE_DB_ID, List.of(coursePlace)))
				.thenReturn(false);
		when(dateCourseRepository.markAsSavedIfAbsent(anyLong(), anyLong(), any(), any()))
				.thenReturn(1);

		saveService.save(ROOM_PUBLIC_ID, DATE_COURSE_ID, "코스", null, USER_ID);

		verify(dateCoursePlaceRepository).findWithRoomPlacesByCourseIdForUpdate(COURSE_DB_ID);
		verify(roomPlaceRepository).findAllByIdInAndRoomIdForUpdate(List.of(1L), ROOM_ID);
		verify(dateCourseRepository).markAsSavedIfAbsent(eq(COURSE_DB_ID), eq(USER_ID), any(), any());
	}

	@Test
	void saveShouldReturnNotFoundWhenCourseIsDeletedBeforeMarkingSaved() {
		Room room = mockRoom();
		DateCourse course = mockUnsavedCourse();
		when(course.getId()).thenReturn(COURSE_DB_ID);
		RoomPlace roomPlace = mockRoomPlace(1L);
		DateCoursePlace coursePlace = mock(DateCoursePlace.class);
		when(coursePlace.getRoomPlace()).thenReturn(roomPlace);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(DATE_COURSE_ID, ROOM_ID))
				.thenReturn(Optional.of(course));
		when(dateCoursePlaceRepository.findWithRoomPlacesByCourseIdForUpdate(COURSE_DB_ID))
				.thenReturn(List.of(coursePlace));
		when(roomPlaceRepository.findAllByIdInAndRoomIdForUpdate(List.of(1L), ROOM_ID))
				.thenReturn(List.of(roomPlace));
		when(duplicatePolicy.existsSavedCourseWithSamePlacesExcluding(ROOM_ID, COURSE_DB_ID, List.of(coursePlace)))
				.thenReturn(false);
		when(dateCourseRepository.markAsSavedIfAbsent(eq(COURSE_DB_ID), eq(USER_ID), any(), any()))
				.thenReturn(0);
		when(dateCourseRepository.existsByIdAndDeletedAtIsNull(COURSE_DB_ID))
				.thenReturn(false);

		assertThatThrownBy(() -> saveService.save(
				ROOM_PUBLIC_ID, DATE_COURSE_ID, "코스", null, USER_ID))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.DATE_COURSE_NOT_FOUND));
	}

	@Test
	void saveShouldReturnAlreadySavedWhenActiveCourseWasMarkedSavedConcurrently() {
		Room room = mockRoom();
		DateCourse course = mockUnsavedCourse();
		when(course.getId()).thenReturn(COURSE_DB_ID);
		RoomPlace roomPlace = mockRoomPlace(1L);
		DateCoursePlace coursePlace = mock(DateCoursePlace.class);
		when(coursePlace.getRoomPlace()).thenReturn(roomPlace);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(DATE_COURSE_ID, ROOM_ID))
				.thenReturn(Optional.of(course));
		when(dateCoursePlaceRepository.findWithRoomPlacesByCourseIdForUpdate(COURSE_DB_ID))
				.thenReturn(List.of(coursePlace));
		when(roomPlaceRepository.findAllByIdInAndRoomIdForUpdate(List.of(1L), ROOM_ID))
				.thenReturn(List.of(roomPlace));
		when(duplicatePolicy.existsSavedCourseWithSamePlacesExcluding(ROOM_ID, COURSE_DB_ID, List.of(coursePlace)))
				.thenReturn(false);
		when(dateCourseRepository.markAsSavedIfAbsent(eq(COURSE_DB_ID), eq(USER_ID), any(), any()))
				.thenReturn(0);
		when(dateCourseRepository.existsByIdAndDeletedAtIsNull(COURSE_DB_ID))
				.thenReturn(true);

		assertThatThrownBy(() -> saveService.save(
				ROOM_PUBLIC_ID, DATE_COURSE_ID, "코스", null, USER_ID))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.DATE_COURSE_ALREADY_SAVED));
	}

	@Test
	void saveRejectsAlreadySavedCourseBeforeReplacingPlaces() {
		Room room = mockRoom();
		DateCourse course = mock(DateCourse.class);
		when(course.getSavedByUserId()).thenReturn(USER_ID);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(DATE_COURSE_ID, ROOM_ID))
				.thenReturn(Optional.of(course));

		assertThatThrownBy(() -> saveService.save(
				ROOM_PUBLIC_ID, DATE_COURSE_ID, "코스", List.of(1L), USER_ID))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.DATE_COURSE_ALREADY_SAVED));

		verify(dateCoursePlaceRepository, never()).deleteByDateCourseId(anyLong());
		verify(dateCourseRepository, never()).markAsSavedIfAbsent(anyLong(), anyLong(), any(), any());
	}

	private Room mockRoom() {
		Room room = mock(Room.class);
		when(room.getId()).thenReturn(ROOM_ID);
		return room;
	}

	private DateCourse mockUnsavedCourse() {
		DateCourse course = mock(DateCourse.class);
		when(course.getSavedByUserId()).thenReturn(null);
		return course;
	}

	private RoomPlace mockRoomPlace(Long id) {
		RoomPlace roomPlace = mock(RoomPlace.class);
		when(roomPlace.getId()).thenReturn(id);
		return roomPlace;
	}
}
