package com.hufs.capstone.backend.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.course.application.dto.DateCourseResult;
import com.hufs.capstone.backend.course.domain.repository.DateCoursePlaceRepository;
import com.hufs.capstone.backend.course.domain.repository.DateCourseRepository;
import com.hufs.capstone.backend.global.exception.FieldValidationException;
import com.hufs.capstone.backend.global.pagination.CursorPageResult;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.region.application.dto.RegionOptionResult;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class DateCourseQueryServiceTest {

	private static final String ROOM_PUBLIC_ID = "room-public-id";
	private static final Long USER_ID = 100L;

	private final RoomAccessService roomAccessService = mock(RoomAccessService.class);
	private final DateCourseRepository dateCourseRepository = mock(DateCourseRepository.class);
	private final DateCoursePlaceRepository dateCoursePlaceRepository = mock(DateCoursePlaceRepository.class);
	private final RoomPlaceRepository roomPlaceRepository = mock(RoomPlaceRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
	private final DateCourseQueryService service = new DateCourseQueryService(
			roomAccessService,
			dateCourseRepository,
			dateCoursePlaceRepository,
			roomPlaceRepository,
			userRepository,
			objectMapper
	);

	@Test
	void listCourseGenerationSidosReturnsOnlyRoomPlaceSidos() {
		Room room = mock(Room.class);
		when(room.getId()).thenReturn(10L);
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(roomPlaceRepository.findDistinctSidoOptionsByRoomId(10L)).thenReturn(List.of(
				region("11", "Seoul"),
				region("41", "Gyeonggi"),
				region("50", "Jeju")
		));

		List<RegionOptionResult> results = service.listCourseGenerationSidos(ROOM_PUBLIC_ID, USER_ID);

		assertThat(results).extracting(RegionOptionResult::code)
				.containsExactly("11", "41", "50");
		verify(roomPlaceRepository).findDistinctSidoOptionsByRoomId(10L);
	}

	@Test
	void listCourseGenerationSigungusReturnsOnlyRoomPlaceSigungusUnderSido() {
		Room room = mock(Room.class);
		when(room.getId()).thenReturn(10L);
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(roomPlaceRepository.existsByRoomIdAndSidoCode(10L, "11")).thenReturn(true);
		when(roomPlaceRepository.findDistinctSigunguOptionsByRoomIdAndSidoCode(10L, "11"))
				.thenReturn(List.of(
						region("11110", "Jongno-gu"),
						region("11680", "Gangnam-gu")
				));

		List<RegionOptionResult> results = service.listCourseGenerationSigungus(ROOM_PUBLIC_ID, "11", USER_ID);

		assertThat(results).extracting(RegionOptionResult::code)
				.containsExactly("11110", "11680");
		verify(roomPlaceRepository).findDistinctSigunguOptionsByRoomIdAndSidoCode(10L, "11");
	}

	@Test
	void listCourseGenerationSigungusRejectsSidoNotSavedInRoom() {
		Room room = mock(Room.class);
		when(room.getId()).thenReturn(10L);
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(roomPlaceRepository.existsByRoomIdAndSidoCode(10L, "41")).thenReturn(false);

		assertThatThrownBy(() -> service.listCourseGenerationSigungus(ROOM_PUBLIC_ID, "41", USER_ID))
				.isInstanceOf(FieldValidationException.class)
				.satisfies(ex -> assertThat(((FieldValidationException) ex).getFieldErrors().get(0).message())
						.isEqualTo("이 방에 저장된 시/도가 아닙니다."));
	}

	@Test
	void listSavedCoursesReturnsCursorPage() {
		Room room = mock(Room.class);
		when(room.getId()).thenReturn(10L);
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		com.hufs.capstone.backend.course.domain.entity.DateCourse first = savedCourse(
				1L,
				"course-1",
				Instant.parse("2026-06-11T02:00:00Z")
		);
		com.hufs.capstone.backend.course.domain.entity.DateCourse second = savedCourse(
				2L,
				"course-2",
				Instant.parse("2026-06-11T01:00:00Z")
		);
		when(dateCourseRepository.findSavedByRoomIdAfterCursor(10L, null, null, PageRequest.of(0, 2)))
				.thenReturn(List.of(first, second));
		when(dateCourseRepository.countSavedByRoomId(10L)).thenReturn(2L);
		when(dateCoursePlaceRepository.findWithRoomPlacesByCourseIdIn(List.of(1L))).thenReturn(List.of());

		CursorPageResult<DateCourseResult> result = service.listSavedCourses(ROOM_PUBLIC_ID, USER_ID, 1, (String) null);

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).dateCourseId()).isEqualTo("course-1");
		assertThat(result.limit()).isEqualTo(1);
		assertThat(result.totalCount()).isEqualTo(2);
		assertThat(result.hasNext()).isTrue();
		assertThat(result.nextCursor()).isNotBlank();
	}

	private static RoomPlaceRepository.RoomPlaceRegionOption region(String code, String name) {
		return new RoomPlaceRepository.RoomPlaceRegionOption() {

			@Override
			public String getCode() {
				return code;
			}

			@Override
			public String getName() {
				return name;
			}
		};
	}

	private static com.hufs.capstone.backend.course.domain.entity.DateCourse savedCourse(
			Long id,
			String dateCourseId,
			Instant savedAt
	) {
		com.hufs.capstone.backend.course.domain.entity.DateCourse course =
				mock(com.hufs.capstone.backend.course.domain.entity.DateCourse.class);
		when(course.getId()).thenReturn(id);
		when(course.getDateCourseId()).thenReturn(dateCourseId);
		when(course.getCourseName()).thenReturn("Course");
		when(course.getGenerationBatchId()).thenReturn("batch");
		when(course.getStartDateTime()).thenReturn(Instant.parse("2026-06-11T00:00:00Z"));
		when(course.getEndDateTime()).thenReturn(Instant.parse("2026-06-11T03:00:00Z"));
		when(course.getCreatedAt()).thenReturn(Instant.parse("2026-06-10T00:00:00Z"));
		when(course.getSavedAt()).thenReturn(savedAt);
		return course;
	}
}
