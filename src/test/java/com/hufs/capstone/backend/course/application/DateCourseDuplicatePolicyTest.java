package com.hufs.capstone.backend.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.course.domain.entity.DateCourse;
import com.hufs.capstone.backend.course.domain.entity.DateCoursePlace;
import com.hufs.capstone.backend.course.domain.repository.DateCoursePlaceRepository;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.util.List;
import org.junit.jupiter.api.Test;

class DateCourseDuplicatePolicyTest {

	private final DateCoursePlaceRepository repository = mock(DateCoursePlaceRepository.class);
	private final DateCourseDuplicatePolicy policy = new DateCourseDuplicatePolicy(repository);

	@Test
	void sameRoomPlaceOrderIsDuplicate() {
		List<DateCoursePlace> savedPlaces = List.of(
				dateCoursePlace(10L, 0, 100L),
				dateCoursePlace(10L, 1, 200L)
		);
		when(repository.findSavedPlacesByRoomId(1L)).thenReturn(savedPlaces);

		boolean duplicate = policy.existsSavedCourseWithSamePlaces(1L, List.of(
				roomPlace(100L),
				roomPlace(200L)
		));

		assertThat(duplicate).isTrue();
	}

	@Test
	void samePlacesWithDifferentOrderAreNotDuplicate() {
		List<DateCoursePlace> savedPlaces = List.of(
				dateCoursePlace(10L, 0, 100L),
				dateCoursePlace(10L, 1, 200L)
		);
		when(repository.findSavedPlacesByRoomId(1L)).thenReturn(savedPlaces);

		boolean duplicate = policy.existsSavedCourseWithSamePlaces(1L, List.of(
				roomPlace(200L),
				roomPlace(100L)
		));

		assertThat(duplicate).isFalse();
	}

	@Test
	void excludingCurrentCourseStillDetectsOtherSavedDuplicate() {
		List<DateCoursePlace> savedPlaces = List.of(
				dateCoursePlace(10L, 0, 100L),
				dateCoursePlace(10L, 1, 200L)
		);
		when(repository.findSavedPlacesByRoomIdExcludingCourseId(1L, 20L)).thenReturn(savedPlaces);

		boolean duplicate = policy.existsSavedCourseWithSamePlacesExcluding(1L, 20L, List.of(
				dateCoursePlace(20L, 0, 100L),
				dateCoursePlace(20L, 1, 200L)
		));

		assertThat(duplicate).isTrue();
	}

	// ────────────────────────────────────────────
	// existsSavedCourseWithSameRoomPlacesExcluding (수정 API 전용)
	// ────────────────────────────────────────────

	@Test
	void editUpdate_동일한_RoomPlace_순서_중복_감지() {
		List<DateCoursePlace> savedPlaces = List.of(
				dateCoursePlace(10L, 0, 100L),
				dateCoursePlace(10L, 1, 200L)
		);
		when(repository.findSavedPlacesByRoomIdExcludingCourseId(1L, 20L)).thenReturn(savedPlaces);

		boolean duplicate = policy.existsSavedCourseWithSameRoomPlacesExcluding(1L, 20L, List.of(
				roomPlace(100L),
				roomPlace(200L)
		));

		assertThat(duplicate).isTrue();
	}

	@Test
	void editUpdate_순서가_다르면_중복_아님() {
		List<DateCoursePlace> savedPlaces = List.of(
				dateCoursePlace(10L, 0, 100L),
				dateCoursePlace(10L, 1, 200L)
		);
		when(repository.findSavedPlacesByRoomIdExcludingCourseId(1L, 20L)).thenReturn(savedPlaces);

		boolean duplicate = policy.existsSavedCourseWithSameRoomPlacesExcluding(1L, 20L, List.of(
				roomPlace(200L),
				roomPlace(100L)
		));

		assertThat(duplicate).isFalse();
	}

	private static DateCoursePlace dateCoursePlace(Long courseId, int sequenceOrder, Long roomPlaceId) {
		DateCourse course = mock(DateCourse.class);
		when(course.getId()).thenReturn(courseId);
		DateCoursePlace dateCoursePlace = mock(DateCoursePlace.class);
		RoomPlace roomPlace = roomPlace(roomPlaceId);
		when(dateCoursePlace.getDateCourse()).thenReturn(course);
		when(dateCoursePlace.getSequenceOrder()).thenReturn(sequenceOrder);
		when(dateCoursePlace.getRoomPlace()).thenReturn(roomPlace);
		return dateCoursePlace;
	}

	private static RoomPlace roomPlace(Long id) {
		RoomPlace roomPlace = mock(RoomPlace.class);
		when(roomPlace.getId()).thenReturn(id);
		return roomPlace;
	}
}
