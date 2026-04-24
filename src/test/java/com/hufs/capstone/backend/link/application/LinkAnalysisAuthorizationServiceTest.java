package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.domain.repository.LinkAnalysisRequestRepository;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LinkAnalysisAuthorizationServiceTest {

	private static final String ROOM_PUBLIC_ID = "11111111-1111-1111-1111-111111111111";

	@Mock
	private RoomAccessService roomAccessService;

	@Mock
	private LinkAnalysisRequestRepository linkAnalysisRequestRepository;

	@InjectMocks
	private LinkAnalysisAuthorizationService linkAnalysisAuthorizationService;

	@Test
	void assertReadableShouldPassWhenRoomMemberAndRequestExists() {
		Room room = room();
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, 100L)).thenReturn(room);
		when(linkAnalysisRequestRepository.existsByRoomAndLinkId(room, 10L)).thenReturn(true);

		linkAnalysisAuthorizationService.assertReadable(100L, ROOM_PUBLIC_ID, 10L);
	}

	@Test
	void assertReadableShouldThrowForbiddenWhenRequestDoesNotExistInRoom() {
		Room room = room();
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, 100L)).thenReturn(room);
		when(linkAnalysisRequestRepository.existsByRoomAndLinkId(room, 10L)).thenReturn(false);

		assertThatThrownBy(() -> linkAnalysisAuthorizationService.assertReadable(100L, ROOM_PUBLIC_ID, 10L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E403_FORBIDDEN));
	}

	private static Room room() {
		Room room = Room.create(ROOM_PUBLIC_ID, "Test Room", "INVITE123456", 100L);
		ReflectionTestUtils.setField(room, "id", 1L);
		return room;
	}
}
