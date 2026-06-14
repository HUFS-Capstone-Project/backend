package com.hufs.capstone.backend.room.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.room.api.response.RoomDetailResponse;
import com.hufs.capstone.backend.room.api.response.RoomSummaryResponse;
import com.hufs.capstone.backend.room.application.dto.RoomDetailResult;
import com.hufs.capstone.backend.room.application.dto.RoomMemberProfileResult;
import com.hufs.capstone.backend.room.application.dto.RoomSummaryResult;
import com.hufs.capstone.backend.room.application.port.RoomLinkCountPort;
import com.hufs.capstone.backend.room.application.port.RoomMemberUserProfilePort;
import com.hufs.capstone.backend.room.application.port.RoomMemberUserProfilePort.RoomMemberUserProfile;
import com.hufs.capstone.backend.room.application.port.RoomPlaceCountPort;
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
	private RoomLinkCountPort roomLinkCountPort;

	@Mock
	private RoomPlaceCountPort roomPlaceCountPort;

	@Mock
	private RoomMemberUserProfilePort roomMemberUserProfilePort;

	@InjectMocks
	private RoomQueryService roomQueryService;

	@Test
	void getMyRoomsShouldReturnJoinedRooms() {
		Room room = room("11111111-1111-1111-1111-111111111111", "Test Room");
		RoomMember member = RoomMember.join(room, USER_ID);
		member.updatePinned(true);
		when(roomMemberRepository.findMyRooms(USER_ID, null)).thenReturn(List.of(member));
		when(roomMemberRepository.countByRoomId(room.getId())).thenReturn(3L);
		when(roomLinkCountPort.countLinksInRoom(room.getId())).thenReturn(2L);
		when(roomPlaceCountPort.countPlacesInRoom(room.getId())).thenReturn(7L);

		List<RoomSummaryResult> result = roomQueryService.getMyRooms(USER_ID);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).roomId()).isEqualTo(room.getPublicId());
		assertThat(result.get(0).roomName()).isEqualTo("Test Room");
		assertThat(result.get(0).inviteCode()).isEqualTo("INVITE123456");
		assertThat(result.get(0).avatarSeed()).isEqualTo(room.getAvatarSeed());
		assertThat(RoomSummaryResponse.from(result.get(0)).avatarSeed()).isEqualTo(room.getAvatarSeed());
		assertThat(result.get(0).pinned()).isTrue();
		assertThat(result.get(0).memberCount()).isEqualTo(3L);
		assertThat(result.get(0).linkCount()).isEqualTo(2L);
		assertThat(result.get(0).placeCount()).isEqualTo(7L);
	}

	@Test
	void getMyRoomsShouldTrimKeyword() {
		Room room = room("11111111-1111-1111-1111-111111111111", "Dinner Room");
		RoomMember member = RoomMember.join(room, USER_ID);
		when(roomMemberRepository.findMyRooms(USER_ID, "dinner")).thenReturn(List.of(member));

		List<RoomSummaryResult> result = roomQueryService.getMyRooms(USER_ID, "  dinner  ");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).roomName()).isEqualTo("Dinner Room");
	}

	@Test
	void getMyRoomsShouldTreatBlankKeywordAsEmptySearch() {
		Room room = room("11111111-1111-1111-1111-111111111111", "Test Room");
		RoomMember member = RoomMember.join(room, USER_ID);
		when(roomMemberRepository.findMyRooms(USER_ID, null)).thenReturn(List.of(member));

		List<RoomSummaryResult> result = roomQueryService.getMyRooms(USER_ID, "   ");

		assertThat(result).hasSize(1);
	}

	@Test
	void getRoomDetailShouldReturnPinnedStatus() {
		Room room = room("11111111-1111-1111-1111-111111111111", "Test Room");
		RoomMember member = RoomMember.join(room, USER_ID);
		member.updatePinned(true);
		when(roomAccessService.getRoomOrThrow(room.getPublicId())).thenReturn(room);
		when(roomAccessService.getMembershipOrThrow(room, USER_ID)).thenReturn(member);
		when(roomMemberRepository.countByRoomId(room.getId())).thenReturn(3L);
		when(roomLinkCountPort.countLinksInRoom(room.getId())).thenReturn(5L);
		when(roomPlaceCountPort.countPlacesInRoom(room.getId())).thenReturn(9L);

		RoomDetailResult result = roomQueryService.getRoomDetail(USER_ID, room.getPublicId());

		assertThat(result.roomId()).isEqualTo(room.getPublicId());
		assertThat(result.pinned()).isTrue();
		assertThat(result.inviteCode()).isEqualTo("INVITE123456");
		assertThat(result.avatarSeed()).isEqualTo(room.getAvatarSeed());
		assertThat(RoomDetailResponse.from(result).avatarSeed()).isEqualTo(room.getAvatarSeed());
		assertThat(result.memberCount()).isEqualTo(3L);
		assertThat(result.linkCount()).isEqualTo(5L);
		assertThat(result.placeCount()).isEqualTo(9L);
	}

	@Test
	void getRoomDetailShouldThrowForbiddenForNonMember() {
		Room room = room("11111111-1111-1111-1111-111111111111", "Test Room");
		when(roomAccessService.getRoomOrThrow(room.getPublicId())).thenReturn(room);
		when(roomAccessService.getMembershipOrThrow(room, USER_ID))
				.thenThrow(new BusinessException(ErrorCode.ROOM_ACCESS_FORBIDDEN));

		assertThatThrownBy(() -> roomQueryService.getRoomDetail(USER_ID, room.getPublicId()))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.ROOM_ACCESS_FORBIDDEN));
	}

	@Test
	void getRoomMembersShouldReturnMemberProfiles() {
		Room room = room("11111111-1111-1111-1111-111111111111", "Test Room");
		RoomMember me = RoomMember.join(room, USER_ID);
		RoomMember friend = RoomMember.join(room, 200L);
		ReflectionTestUtils.setField(me, "createdAt", Instant.parse("2026-04-16T00:00:00Z"));
		ReflectionTestUtils.setField(friend, "createdAt", Instant.parse("2026-04-17T00:00:00Z"));
		when(roomAccessService.requireMemberRoom(room.getPublicId(), USER_ID)).thenReturn(room);
		when(roomMemberRepository.findByRoomIdOrderByCreatedAtAscIdAsc(room.getId())).thenReturn(List.of(me, friend));
		when(roomMemberUserProfilePort.findActiveProfiles(List.of(USER_ID, 200L))).thenReturn(List.of(
				new RoomMemberUserProfile(USER_ID, "me", "https://example.com/me.png"),
				new RoomMemberUserProfile(200L, "friend", "https://example.com/friend.png")
		));

		List<RoomMemberProfileResult> result = roomQueryService.getRoomMembers(USER_ID, room.getPublicId());

		assertThat(result).hasSize(2);
		assertThat(result.get(0).userId()).isEqualTo(USER_ID);
		assertThat(result.get(0).nickname()).isEqualTo("me");
		assertThat(result.get(0).profileImageUrl()).isEqualTo("https://example.com/me.png");
		assertThat(result.get(0).me()).isTrue();
		assertThat(result.get(1).userId()).isEqualTo(200L);
		assertThat(result.get(1).nickname()).isEqualTo("friend");
		assertThat(result.get(1).me()).isFalse();
	}

	private static Room room(String publicId, String name) {
		Room room = Room.create(publicId, name, "INVITE123456", USER_ID);
		ReflectionTestUtils.setField(room, "id", 1L);
		ReflectionTestUtils.setField(room, "createdAt", Instant.parse("2026-04-16T00:00:00Z"));
		return room;
	}
}
