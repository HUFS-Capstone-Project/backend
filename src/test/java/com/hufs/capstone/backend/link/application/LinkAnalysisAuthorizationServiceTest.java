package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkAnalysisRequest;
import com.hufs.capstone.backend.link.domain.repository.LinkAnalysisRequestRepository;
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
	private LinkAnalysisRequestRepository linkAnalysisRequestRepository;

	@InjectMocks
	private LinkAnalysisAuthorizationService linkAnalysisAuthorizationService;

	@Test
	void requireAnalysisRequestShouldPassWhenRoomMemberAndRequestBelongsToRoom() {
		Room room = room(1L, ROOM_PUBLIC_ID);
		LinkAnalysisRequest analysisRequest = analysisRequest(10L, room);
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, 100L)).thenReturn(room);
		when(linkAnalysisRequestRepository.findWithRoomAndLinkById(10L)).thenReturn(Optional.of(analysisRequest));

		LinkAnalysisRequest result = linkAnalysisAuthorizationService.requireAnalysisRequest(100L, ROOM_PUBLIC_ID, 10L);

		assertThat(result).isEqualTo(analysisRequest);
	}

	@Test
	void requireAnalysisRequestShouldThrowForbiddenWhenRequestBelongsToDifferentRoom() {
		Room requestedRoom = room(1L, ROOM_PUBLIC_ID);
		Room otherRoom = room(2L, "22222222-2222-2222-2222-222222222222");
		LinkAnalysisRequest analysisRequest = analysisRequest(10L, otherRoom);
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, 100L)).thenReturn(requestedRoom);
		when(linkAnalysisRequestRepository.findWithRoomAndLinkById(10L)).thenReturn(Optional.of(analysisRequest));

		assertThatThrownBy(() -> linkAnalysisAuthorizationService.requireAnalysisRequest(100L, ROOM_PUBLIC_ID, 10L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E403_FORBIDDEN));
	}

	@Test
	void requireAnalysisRequestForUpdateShouldUseLockedLookup() {
		Room room = room(1L, ROOM_PUBLIC_ID);
		LinkAnalysisRequest analysisRequest = analysisRequest(10L, room);
		when(roomAccessService.requireMemberRoom(ROOM_PUBLIC_ID, 100L)).thenReturn(room);
		when(linkAnalysisRequestRepository.findWithRoomAndLinkByIdForUpdate(10L)).thenReturn(Optional.of(analysisRequest));

		LinkAnalysisRequest result =
				linkAnalysisAuthorizationService.requireAnalysisRequestForUpdate(100L, ROOM_PUBLIC_ID, 10L);

		assertThat(result).isEqualTo(analysisRequest);
	}

	private static LinkAnalysisRequest analysisRequest(Long id, Room room) {
		Link link = Link.register("https://example.com/post/" + id, "https://example.com/post/" + id, "job-" + id);
		ReflectionTestUtils.setField(link, "id", id + 100);
		LinkAnalysisRequest analysisRequest = LinkAnalysisRequest.create(link, room, 100L, null);
		ReflectionTestUtils.setField(analysisRequest, "id", id);
		return analysisRequest;
	}

	private static Room room(Long id, String publicId) {
		Room room = Room.create(publicId, "Test Room", "INVITE123456", 100L);
		ReflectionTestUtils.setField(room, "id", id);
		return room;
	}
}
