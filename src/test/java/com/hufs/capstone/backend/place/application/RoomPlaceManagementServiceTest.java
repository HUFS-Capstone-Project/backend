package com.hufs.capstone.backend.place.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.course.domain.repository.DateCoursePlaceRepository;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceMemoRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceOriginRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class RoomPlaceManagementServiceTest {

	private static final Long USER_ID = 100L;
	private static final Long ROOM_ID = 10L;
	private static final Long ROOM_PLACE_ID = 20L;
	private static final String ROOM_PUBLIC_ID = "room-public-id";

	@Mock
	private RoomAccessService roomAccessService;

	@Mock
	private RoomPlaceRepository roomPlaceRepository;

	@Mock
	private RoomPlaceMemoRepository roomPlaceMemoRepository;

	@Mock
	private RoomPlaceOriginRepository roomPlaceOriginRepository;

	@Mock
	private DateCoursePlaceRepository dateCoursePlaceRepository;

	@InjectMocks
	private RoomPlaceManagementService roomPlaceManagementService;

	@Test
	void deleteRoomPlaceShouldTranslateFkConflictToBusinessError() {
		Room room = mock(Room.class);
		RoomPlace roomPlace = mock(RoomPlace.class);
		when(room.getId()).thenReturn(ROOM_ID);
		when(roomPlace.getId()).thenReturn(ROOM_PLACE_ID);
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, USER_ID)).thenReturn(room);
		when(roomPlaceRepository.findByIdAndRoomIdForUpdate(ROOM_PLACE_ID, ROOM_ID))
				.thenReturn(Optional.of(roomPlace));
		when(dateCoursePlaceRepository.existsByRoomPlaceIdInSavedDateCourse(ROOM_PLACE_ID)).thenReturn(false);
		doThrow(new DataIntegrityViolationException("fk"))
				.when(roomPlaceRepository)
				.flush();

		assertThatThrownBy(() -> roomPlaceManagementService.deleteRoomPlace(USER_ID, ROOM_PUBLIC_ID, ROOM_PLACE_ID))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.ROOM_PLACE_USED_IN_DATE_COURSE));

		verify(dateCoursePlaceRepository).deleteByRoomPlaceId(ROOM_PLACE_ID);
		verify(roomPlaceMemoRepository).deleteByRoomPlaceId(ROOM_PLACE_ID);
		verify(roomPlaceOriginRepository).deleteByRoomPlaceId(ROOM_PLACE_ID);
		verify(roomPlaceRepository).delete(roomPlace);
	}
}
