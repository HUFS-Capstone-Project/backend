package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.Optional;
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
	private RoomLinkRepository roomLinkRepository;

	@InjectMocks
	private LinkAnalysisAuthorizationService linkAnalysisAuthorizationService;

	@Test
	void requireReadableRoomShouldPassWhenRoomMemberAndRoomLinkExists() {
		Room room = room();
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, 100L)).thenReturn(room);
		when(roomLinkRepository.existsByRoomAndLinkId(room, 10L)).thenReturn(true);

		Room result = linkAnalysisAuthorizationService.requireReadableRoom(100L, ROOM_PUBLIC_ID, 10L);

		assertThat(result).isEqualTo(room);
	}

	@Test
	void requireReadableRoomShouldThrowForbiddenWhenRoomLinkDoesNotExist() {
		Room room = room();
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, 100L)).thenReturn(room);
		when(roomLinkRepository.existsByRoomAndLinkId(room, 10L)).thenReturn(false);

		assertThatThrownBy(() -> linkAnalysisAuthorizationService.requireReadableRoom(100L, ROOM_PUBLIC_ID, 10L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E403_FORBIDDEN));
	}

	@Test
	void requireRoomLinkShouldReturnRoomLinkWhenSharedInRoom() {
		Room room = room();
		Link link = Link.register("https://example.com/post/1", "https://example.com/post/1", "job-1");
		ReflectionTestUtils.setField(link, "id", 10L);
		RoomLink roomLink = RoomLink.bind(room, link);
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, 100L)).thenReturn(room);
		when(roomLinkRepository.existsByRoomAndLinkId(room, 10L)).thenReturn(true);
		when(roomLinkRepository.findByRoomAndLinkId(room, 10L)).thenReturn(Optional.of(roomLink));

		RoomLink result = linkAnalysisAuthorizationService.requireRoomLink(100L, ROOM_PUBLIC_ID, 10L);

		assertThat(result).isEqualTo(roomLink);
	}

	private static Room room() {
		Room room = Room.create(ROOM_PUBLIC_ID, "Test Room", "INVITE123456", 100L);
		ReflectionTestUtils.setField(room, "id", 1L);
		return room;
	}
}
