package com.hufs.capstone.backend.room.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.room.application.dto.RoomDetailResult;
import com.hufs.capstone.backend.room.application.dto.RoomSummaryResult;
import com.hufs.capstone.backend.room.domain.RoomMemberRole;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import com.hufs.capstone.backend.room.domain.repository.RoomMemberRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoomQueryServiceTest {

	private static final Long USER_ID = 100L;

	@Mock
	private RoomAccessService roomAccessService;

	@Mock
	private RoomMemberRepository roomMemberRepository;

	@Mock
	private RoomLinkRepository roomLinkRepository;

	@InjectMocks
	private RoomQueryService roomQueryService;

	@Test
	void getMyRoomsShouldReturnJoinedRooms() {
		Room room = room("11111111-1111-1111-1111-111111111111", "테스트 방");
		RoomMember member = RoomMember.owner(room, USER_ID);
		when(roomMemberRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(member));
		when(roomMemberRepository.countByRoomId(room.getId())).thenReturn(3L);
		when(roomLinkRepository.countByRoomId(room.getId())).thenReturn(2L);

		List<RoomSummaryResult> result = roomQueryService.getMyRooms(USER_ID);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).roomId()).isEqualTo(room.getPublicId());
		assertThat(result.get(0).roomName()).isEqualTo("테스트 방");
		assertThat(result.get(0).memberCount()).isEqualTo(3L);
		assertThat(result.get(0).linkCount()).isEqualTo(2L);
	}

	@Test
	void getRoomDetailShouldExposeInviteCodeForOwner() {
		Room room = room("11111111-1111-1111-1111-111111111111", "테스트 방");
		RoomMember owner = RoomMember.owner(room, USER_ID);
		when(roomAccessService.getRoomOrThrow(room.getPublicId())).thenReturn(room);
		when(roomAccessService.getMembershipOrThrow(room, USER_ID)).thenReturn(owner);
		when(roomMemberRepository.countByRoomId(room.getId())).thenReturn(3L);
		when(roomLinkRepository.countByRoomId(room.getId())).thenReturn(5L);

		RoomDetailResult result = roomQueryService.getRoomDetail(USER_ID, room.getPublicId());

		assertThat(result.roomId()).isEqualTo(room.getPublicId());
		assertThat(result.role()).isEqualTo(RoomMemberRole.OWNER);
		assertThat(result.inviteCode()).isEqualTo("INVITE123456");
		assertThat(result.memberCount()).isEqualTo(3L);
		assertThat(result.linkCount()).isEqualTo(5L);
	}

	@Test
	void getRoomDetailShouldExposeInviteCodeForMember() {
		Room room = room("11111111-1111-1111-1111-111111111111", "테스트 방");
		RoomMember member = RoomMember.member(room, USER_ID);
		when(roomAccessService.getRoomOrThrow(room.getPublicId())).thenReturn(room);
		when(roomAccessService.getMembershipOrThrow(room, USER_ID)).thenReturn(member);
		when(roomMemberRepository.countByRoomId(room.getId())).thenReturn(3L);
		when(roomLinkRepository.countByRoomId(room.getId())).thenReturn(5L);

		RoomDetailResult result = roomQueryService.getRoomDetail(USER_ID, room.getPublicId());

		assertThat(result.role()).isEqualTo(RoomMemberRole.MEMBER);
		assertThat(result.inviteCode()).isEqualTo("INVITE123456");
		assertThat(result.memberCount()).isEqualTo(3L);
	}

	@Test
	void getRoomDetailShouldThrowForbiddenForNonMember() {
		Room room = room("11111111-1111-1111-1111-111111111111", "테스트 방");
		when(roomAccessService.getRoomOrThrow(room.getPublicId())).thenReturn(room);
		when(roomAccessService.getMembershipOrThrow(room, USER_ID))
				.thenThrow(new BusinessException(ErrorCode.E403_FORBIDDEN, "방 접근 권한이 없습니다."));

		assertThatThrownBy(() -> roomQueryService.getRoomDetail(USER_ID, room.getPublicId()))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E403_FORBIDDEN));
	}

	private static Room room(String publicId, String name) {
		Room room = Room.create(publicId, name, "INVITE123456", USER_ID);
		ReflectionTestUtils.setField(room, "id", 1L);
		ReflectionTestUtils.setField(room, "createdAt", Instant.parse("2026-04-16T00:00:00Z"));
		return room;
	}
}
