package com.hufs.capstone.backend.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.course.domain.repository.DateCoursePlaceRepository;
import com.hufs.capstone.backend.course.domain.repository.DateCourseRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.region.application.dto.RegionOptionResult;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.user.domain.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class DateCourseQueryServiceTest {

	private static final String ROOM_PUBLIC_ID = "room-public-id";
	private static final Long USER_ID = 100L;

	private final RoomAccessService roomAccessService = mock(RoomAccessService.class);
	private final DateCourseRepository dateCourseRepository = mock(DateCourseRepository.class);
	private final DateCoursePlaceRepository dateCoursePlaceRepository = mock(DateCoursePlaceRepository.class);
	private final RoomPlaceRepository roomPlaceRepository = mock(RoomPlaceRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final ObjectMapper objectMapper = mock(ObjectMapper.class);
	private final DateCourseQueryService service = new DateCourseQueryService(
			roomAccessService,
			dateCourseRepository,
			dateCoursePlaceRepository,
			roomPlaceRepository,
			userRepository,
			objectMapper
	);

	@Test
	void listCourseGenerationSigungusReturnsOnlyRoomPlaceSigungus() {
		Room room = mock(Room.class);
		when(room.getId()).thenReturn(10L);
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(roomPlaceRepository.findDistinctSigunguOptionsByRoomId(10L)).thenReturn(List.of(
				sigungu("11110", "Jongno-gu"),
				sigungu("11680", "Gangnam-gu")
		));

		List<RegionOptionResult> results = service.listCourseGenerationSigungus(ROOM_PUBLIC_ID, USER_ID);

		assertThat(results).extracting(RegionOptionResult::code)
				.containsExactly("11110", "11680");
		assertThat(results).extracting(RegionOptionResult::name)
				.containsExactly("Jongno-gu", "Gangnam-gu");
		assertThat(results).extracting(RegionOptionResult::displayOrder)
				.containsExactly(1, 2);
		assertThat(results).extracting(RegionOptionResult::all)
				.containsExactly(false, false);
		verify(roomAccessService).requireMemberRoom(ROOM_PUBLIC_ID, USER_ID);
		verify(roomPlaceRepository).findDistinctSigunguOptionsByRoomId(10L);
	}

	private static RoomPlaceRepository.RoomPlaceSigunguOption sigungu(String code, String name) {
		return new RoomPlaceRepository.RoomPlaceSigunguOption() {

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
}
