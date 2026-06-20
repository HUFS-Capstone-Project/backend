package com.hufs.capstone.backend.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.course.application.dto.DateCourseResult;
import com.hufs.capstone.backend.course.domain.entity.DateCourse;
import com.hufs.capstone.backend.course.domain.entity.DateCoursePlace;
import com.hufs.capstone.backend.course.domain.repository.DateCoursePlaceRepository;
import com.hufs.capstone.backend.course.domain.repository.DateCourseRepository;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.global.exception.FieldValidationException;
import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.user.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DateCourseEditServiceTest {

	private static final String ROOM_PUBLIC_ID = "room-public-id";
	private static final String COURSE_UUID = "course-uuid-1234";
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
	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private DateCourseEditService editService;

	// ────────────────────────────────────────────
	// 정상 케이스
	// ────────────────────────────────────────────

	@Test
	void updateReplacesCoursePlacesSuccessfully() {
		Room room = mockRoom();
		DateCourse course = mockSavedCourse(USER_ID, USER_ID);
		RoomPlace rp1 = mockRoomPlace(1L);
		RoomPlace rp2 = mockRoomPlace(2L);
		// saveAll은 정상 매핑이 가능한 mock DateCoursePlace 반환
		List<DateCoursePlace> savedDcps = List.of(
				mockDateCoursePlace(rp1, 0),
				mockDateCoursePlace(rp2, 1)
		);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(COURSE_UUID, ROOM_ID))
				.thenReturn(Optional.of(course));
		when(roomPlaceRepository.findAllByIdInAndRoomIdForUpdate(List.of(1L, 2L), ROOM_ID))
				.thenReturn(List.of(rp1, rp2));
		when(duplicatePolicy.existsSavedCourseWithSameRoomPlacesExcluding(anyLong(), anyLong(), any()))
				.thenReturn(false);
		when(dateCoursePlaceRepository.saveAllAndFlush(any())).thenReturn(savedDcps);
		when(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).thenReturn(Optional.empty());

		DateCourseResult result = editService.update(
				ROOM_PUBLIC_ID, COURSE_UUID, "새 코스 이름", List.of(1L, 2L), USER_ID);

		assertThat(result).isNotNull();
		assertThat(result.places()).hasSize(2);
		verify(course).rename("새 코스 이름");
		verify(course).clearSkippedSlots();
		verify(dateCoursePlaceRepository).deleteByDateCourseId(COURSE_DB_ID);
		verify(dateCoursePlaceRepository).saveAllAndFlush(any());
	}

	// ────────────────────────────────────────────
	// 권한 관련
	// ────────────────────────────────────────────

	@Test
	void updateThrows403WhenNeitherCreatorNorSaver() {
		Room room = mockRoom();
		DateCourse course = mockSavedCourse(888L, 999L); // 생성자, 저장자 모두 요청자 아님

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(COURSE_UUID, ROOM_ID))
				.thenReturn(Optional.of(course));

		assertThatThrownBy(() -> editService.update(
				ROOM_PUBLIC_ID, COURSE_UUID, "이름", List.of(1L), USER_ID))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.DATE_COURSE_FORBIDDEN_EDIT));
	}

	@Test
	void updateAllowsCreatorEvenIfDifferentSaver() {
		Room room = mockRoom();
		// 생성자=요청자(100), 저장자=다른 사람(999)
		DateCourse course = mockSavedCourse(USER_ID, 999L);
		RoomPlace rp1 = mockRoomPlace(1L);
		List<DateCoursePlace> savedDcps = List.of(mockDateCoursePlace(rp1, 0));

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(COURSE_UUID, ROOM_ID))
				.thenReturn(Optional.of(course));
		when(roomPlaceRepository.findAllByIdInAndRoomIdForUpdate(List.of(1L), ROOM_ID)).thenReturn(List.of(rp1));
		when(duplicatePolicy.existsSavedCourseWithSameRoomPlacesExcluding(anyLong(), anyLong(), any()))
				.thenReturn(false);
		when(dateCoursePlaceRepository.saveAllAndFlush(any())).thenReturn(savedDcps);
		when(userRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

		DateCourseResult result = editService.update(
				ROOM_PUBLIC_ID, COURSE_UUID, "이름", List.of(1L), USER_ID);

		assertThat(result).isNotNull();
	}

	@Test
	void updateAllowsSaverEvenIfDifferentCreator() {
		Room room = mockRoom();
		// 생성자=다른 사람(888), 저장자=요청자(100)
		DateCourse course = mockSavedCourse(888L, USER_ID);
		RoomPlace rp1 = mockRoomPlace(1L);
		List<DateCoursePlace> savedDcps = List.of(mockDateCoursePlace(rp1, 0));

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(COURSE_UUID, ROOM_ID))
				.thenReturn(Optional.of(course));
		when(roomPlaceRepository.findAllByIdInAndRoomIdForUpdate(List.of(1L), ROOM_ID)).thenReturn(List.of(rp1));
		when(duplicatePolicy.existsSavedCourseWithSameRoomPlacesExcluding(anyLong(), anyLong(), any()))
				.thenReturn(false);
		when(dateCoursePlaceRepository.saveAllAndFlush(any())).thenReturn(savedDcps);
		when(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).thenReturn(Optional.empty());

		DateCourseResult result = editService.update(
				ROOM_PUBLIC_ID, COURSE_UUID, "이름", List.of(1L), USER_ID);

		assertThat(result).isNotNull();
	}

	// ────────────────────────────────────────────
	// 404 케이스
	// ────────────────────────────────────────────

	@Test
	void updateThrows404WhenCourseNotFound() {
		Room room = mockRoom();
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(COURSE_UUID, ROOM_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> editService.update(
				ROOM_PUBLIC_ID, COURSE_UUID, "이름", List.of(1L), USER_ID))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.DATE_COURSE_NOT_FOUND));
	}

	@Test
	void updateThrows404WhenCourseNotSaved() {
		Room room = mockRoom();
		DateCourse course = mock(DateCourse.class);
		when(course.getSavedByUserId()).thenReturn(null);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(COURSE_UUID, ROOM_ID))
				.thenReturn(Optional.of(course));

		assertThatThrownBy(() -> editService.update(
				ROOM_PUBLIC_ID, COURSE_UUID, "이름", List.of(1L), USER_ID))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.DATE_COURSE_NOT_FOUND));
	}

	// ────────────────────────────────────────────
	// 장소 검증 관련 (400)
	// ────────────────────────────────────────────

	@Test
	void updateThrows400WhenRoomPlaceNotInRoom() {
		Room room = mockRoom();
		DateCourse course = mockSavedCourse(USER_ID, USER_ID);
		// 2개 요청했지만 1개만 조회됨 → 방에 없는 장소 포함
		RoomPlace validRp = mockRoomPlace(1L);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(COURSE_UUID, ROOM_ID))
				.thenReturn(Optional.of(course));
		when(roomPlaceRepository.findAllByIdInAndRoomIdForUpdate(List.of(1L, 999L), ROOM_ID))
				.thenReturn(List.of(validRp));

		assertThatThrownBy(() -> editService.update(
				ROOM_PUBLIC_ID, COURSE_UUID, "이름", List.of(1L, 999L), USER_ID))
				.isInstanceOf(FieldValidationException.class);
	}

	@Test
	void updateThrows400WhenRoomPlaceIsDeletedConcurrently() {
		Room room = mockRoom();
		DateCourse course = mockSavedCourse(USER_ID, USER_ID);
		RoomPlace rp1 = mockRoomPlace(1L);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(COURSE_UUID, ROOM_ID))
				.thenReturn(Optional.of(course));
		when(roomPlaceRepository.findAllByIdInAndRoomIdForUpdate(List.of(1L), ROOM_ID))
				.thenReturn(List.of(rp1));
		when(duplicatePolicy.existsSavedCourseWithSameRoomPlacesExcluding(anyLong(), anyLong(), any()))
				.thenReturn(false);
		when(dateCoursePlaceRepository.saveAllAndFlush(any()))
				.thenThrow(new DataIntegrityViolationException("fk"));

		assertThatThrownBy(() -> editService.update(
				ROOM_PUBLIC_ID, COURSE_UUID, "이름", List.of(1L), USER_ID))
				.isInstanceOf(FieldValidationException.class);
	}

	@Test
	void updateThrows400WhenDuplicateRoomPlaceIds() {
		Room room = mockRoom();
		DateCourse course = mockSavedCourse(USER_ID, USER_ID);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(COURSE_UUID, ROOM_ID))
				.thenReturn(Optional.of(course));

		assertThatThrownBy(() -> editService.update(
				ROOM_PUBLIC_ID, COURSE_UUID, "이름", List.of(1L, 1L), USER_ID))
				.isInstanceOf(FieldValidationException.class);

		// 중복 검증 먼저 → RoomPlace 조회 불필요
		verify(roomPlaceRepository, never()).findAllByIdInAndRoomIdForUpdate(any(), anyLong());
	}

	// ────────────────────────────────────────────
	// 중복 코스 (409 전용 코드)
	// ────────────────────────────────────────────

	@Test
	void updateThrows409WhenDuplicateCourseExists() {
		Room room = mockRoom();
		DateCourse course = mockSavedCourse(USER_ID, USER_ID);
		RoomPlace rp1 = mockRoomPlace(1L);

		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(dateCourseRepository.findByDateCourseIdAndRoomIdAndDeletedAtIsNull(COURSE_UUID, ROOM_ID))
				.thenReturn(Optional.of(course));
		when(roomPlaceRepository.findAllByIdInAndRoomIdForUpdate(List.of(1L), ROOM_ID)).thenReturn(List.of(rp1));
		when(duplicatePolicy.existsSavedCourseWithSameRoomPlacesExcluding(anyLong(), anyLong(), any()))
				.thenReturn(true);

		assertThatThrownBy(() -> editService.update(
				ROOM_PUBLIC_ID, COURSE_UUID, "이름", List.of(1L), USER_ID))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.E409_DUPLICATE_DATE_COURSE));
	}

	// ────────────────────────────────────────────
	// 헬퍼
	// ────────────────────────────────────────────

	private Room mockRoom() {
		Room room = mock(Room.class);
		when(room.getId()).thenReturn(ROOM_ID);
		return room;
	}

	private DateCourse mockSavedCourse(Long createdByUserId, Long savedByUserId) {
		DateCourse course = mock(DateCourse.class);
		when(course.getId()).thenReturn(COURSE_DB_ID);
		when(course.getDateCourseId()).thenReturn(COURSE_UUID);
		when(course.getCourseName()).thenReturn("기존 코스 이름");
		when(course.getSavedByUserId()).thenReturn(savedByUserId);
		when(course.getCreatedByUserId()).thenReturn(createdByUserId);
		when(course.getSavedAt()).thenReturn(Instant.now());
		when(course.getCreatedAt()).thenReturn(Instant.now());
		return course;
	}

	private RoomPlace mockRoomPlace(Long id) {
		RoomPlace rp = mock(RoomPlace.class);
		when(rp.getId()).thenReturn(id);
		return rp;
	}

	/**
	 * DateCoursePlace mapper가 필요로 하는 Place chain을 포함한 mock을 반환한다.
	 * 주의: rp.getPlace()를 포함해 이 메서드 내에서 모든 stubbing을 완결짓는다.
	 * thenReturn() 인자로 호출하면 Mockito 내부 상태가 꼬이므로, 반드시 사전에 변수에 할당해서 사용해야 한다.
	 */
	private DateCoursePlace mockDateCoursePlace(RoomPlace rp, int sequenceOrder) {
		PlaceCategory serviceCategory = mock(PlaceCategory.class);
		when(serviceCategory.getCode()).thenReturn("FD006");
		when(serviceCategory.getName()).thenReturn("음식점");

		PlaceTag serviceTag = mock(PlaceTag.class);
		when(serviceTag.getCode()).thenReturn("tag-1");
		when(serviceTag.getName()).thenReturn("태그");

		Place place = mock(Place.class);
		when(place.getId()).thenReturn(1000L);
		when(place.getKakaoPlaceId()).thenReturn("kakao-test");
		when(place.getName()).thenReturn("테스트 장소");
		when(place.getAddress()).thenReturn("주소");
		when(place.getRoadAddress()).thenReturn("도로명 주소");
		when(place.getLatitude()).thenReturn(BigDecimal.valueOf(37.5));
		when(place.getLongitude()).thenReturn(BigDecimal.valueOf(127.0));
		when(place.getServiceCategory()).thenReturn(serviceCategory);
		when(place.getServiceTag()).thenReturn(serviceTag);

		// rp.getPlace()를 여기서 stubbing
		when(rp.getPlace()).thenReturn(place);

		DateCoursePlace dcp = mock(DateCoursePlace.class);
		when(dcp.getSequenceOrder()).thenReturn(sequenceOrder);
		when(dcp.getRoomPlace()).thenReturn(rp);
		return dcp;
	}
}
