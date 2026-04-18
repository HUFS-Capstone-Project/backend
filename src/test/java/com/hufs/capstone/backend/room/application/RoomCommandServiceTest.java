package com.hufs.capstone.backend.room.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.room.application.dto.CreateRoomResult;
import com.hufs.capstone.backend.room.application.dto.JoinRoomResult;
import com.hufs.capstone.backend.room.application.impl.RoomCommandServiceImpl;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import com.hufs.capstone.backend.room.domain.repository.RoomMemberRepository;
import com.hufs.capstone.backend.room.domain.repository.RoomRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoomCommandServiceTest {

	private static final Long USER_ID = 100L;

	@Mock
	private RoomRepository roomRepository;

	@Mock
	private RoomMemberRepository roomMemberRepository;

	@Mock
	private RoomLinkRepository roomLinkRepository;

	@Mock
	private RoomInviteCodeGenerator inviteCodeGenerator;

	@Mock
	private RoomJoinRateLimiter roomJoinRateLimiter;

	@Mock
	private RoomAccessService roomAccessService;

	@InjectMocks
	private RoomCommandServiceImpl roomCommandService;

	@Test
	void createRoomShouldCreateMembership() {
		when(inviteCodeGenerator.generate()).thenReturn("INVITE123456");
		when(roomRepository.existsByInviteCode("INVITE123456")).thenReturn(false);
		when(roomRepository.saveAndFlush(any(Room.class))).thenAnswer(invocation -> {
			Room saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", 1L);
			ReflectionTestUtils.setField(saved, "createdAt", Instant.parse("2026-04-16T00:00:00Z"));
			return saved;
		});
		when(roomMemberRepository.saveAndFlush(any(RoomMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateRoomResult result = roomCommandService.createRoom(USER_ID, "Test Room");

		assertThat(result.roomName()).isEqualTo("Test Room");
		assertThat(result.inviteCode()).isEqualTo("INVITE123456");
		assertThat(result.pinned()).isFalse();
		verify(roomMemberRepository).saveAndFlush(any(RoomMember.class));
	}

	@Test
	void joinByInviteCodeShouldCreateMembership() {
		Room room = room("11111111-1111-1111-1111-111111111111");
		when(roomJoinRateLimiter.allow(USER_ID, "127.0.0.1")).thenReturn(true);
		when(roomRepository.findByInviteCodeForUpdate("INVITE123456")).thenReturn(Optional.of(room));
		when(roomMemberRepository.existsByRoomAndUserId(room, USER_ID)).thenReturn(false);
		when(roomMemberRepository.countByRoomId(room.getId())).thenReturn(1L);
		when(roomMemberRepository.saveAndFlush(any(RoomMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

		JoinRoomResult result = roomCommandService.joinByInviteCode(USER_ID, "INVITE123456", "127.0.0.1");

		assertThat(result.roomId()).isEqualTo(room.getPublicId());
		assertThat(result.pinned()).isFalse();
	}

	@Test
	void joinByInviteCodeShouldRejectWhenRateLimitExceeded() {
		when(roomJoinRateLimiter.allow(anyLong(), anyString())).thenReturn(false);

		assertThatThrownBy(() -> roomCommandService.joinByInviteCode(USER_ID, "INVITE123456", "127.0.0.1"))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E429_TOO_MANY_REQUESTS));
	}

	@Test
	void joinByInviteCodeShouldRejectWhenInviteCodeInvalid() {
		when(roomJoinRateLimiter.allow(anyLong(), anyString())).thenReturn(true);
		when(roomRepository.findByInviteCodeForUpdate("INVITE123456")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> roomCommandService.joinByInviteCode(USER_ID, "INVITE123456", "127.0.0.1"))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E400_ILLEGAL_ARGUMENT));
	}

	@Test
	void joinByInviteCodeShouldRejectDuplicateMembership() {
		Room room = room("11111111-1111-1111-1111-111111111111");
		when(roomJoinRateLimiter.allow(anyLong(), anyString())).thenReturn(true);
		when(roomRepository.findByInviteCodeForUpdate("INVITE123456")).thenReturn(Optional.of(room));
		when(roomMemberRepository.existsByRoomAndUserId(room, USER_ID)).thenReturn(true);

		assertThatThrownBy(() -> roomCommandService.joinByInviteCode(USER_ID, "INVITE123456", "127.0.0.1"))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E409_CONFLICT));
	}

	@Test
	void joinByInviteCodeShouldRejectWhenRoomIsFull() {
		Room room = room("11111111-1111-1111-1111-111111111111");
		when(roomJoinRateLimiter.allow(anyLong(), anyString())).thenReturn(true);
		when(roomRepository.findByInviteCodeForUpdate("INVITE123456")).thenReturn(Optional.of(room));
		when(roomMemberRepository.existsByRoomAndUserId(room, USER_ID)).thenReturn(false);
		when(roomMemberRepository.countByRoomId(room.getId())).thenReturn(6L);

		assertThatThrownBy(() -> roomCommandService.joinByInviteCode(USER_ID, "INVITE123456", "127.0.0.1"))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E409_CONFLICT));
	}

	@Test
	void renameRoomShouldTrimAndRename() {
		Room room = room("11111111-1111-1111-1111-111111111111");
		RoomMember member = RoomMember.join(room, USER_ID);
		when(roomAccessService.getRoomOrThrow(room.getPublicId())).thenReturn(room);
		when(roomAccessService.getMembershipOrThrow(room, USER_ID)).thenReturn(member);

		roomCommandService.renameRoom(USER_ID, room.getPublicId(), "  새 방 이름  ");

		assertThat(room.getName()).isEqualTo("새 방 이름");
	}

	@Test
	void renameRoomShouldRejectBlankName() {
		assertThatThrownBy(() -> roomCommandService.renameRoom(USER_ID, "room-id", "   "))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E400_ILLEGAL_ARGUMENT));
	}

	@Test
	void renameRoomShouldRejectTooLongName() {
		String tooLongName = "a".repeat(21);

		assertThatThrownBy(() -> roomCommandService.renameRoom(USER_ID, "room-id", tooLongName))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E400_ILLEGAL_ARGUMENT));
	}

	@Test
	void renameRoomShouldThrowWhenUserIsNotMember() {
		Room room = room("11111111-1111-1111-1111-111111111111");
		when(roomAccessService.getRoomOrThrow(room.getPublicId())).thenReturn(room);
		when(roomAccessService.getMembershipOrThrow(room, USER_ID))
				.thenThrow(new BusinessException(ErrorCode.E403_FORBIDDEN, "방 접근 권한이 없습니다."));

		assertThatThrownBy(() -> roomCommandService.renameRoom(USER_ID, room.getPublicId(), "새 방 이름"))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E403_FORBIDDEN));
	}

	@Test
	void updateRoomPinShouldUpdateMembership() {
		Room room = room("11111111-1111-1111-1111-111111111111");
		RoomMember member = RoomMember.join(room, USER_ID);
		when(roomAccessService.getRoomOrThrow(room.getPublicId())).thenReturn(room);
		when(roomAccessService.getMembershipOrThrow(room, USER_ID)).thenReturn(member);

		roomCommandService.updateRoomPin(USER_ID, room.getPublicId(), true);

		assertThat(member.isPinned()).isTrue();
	}

	@Test
	void updateRoomPinShouldThrowWhenUserIsNotMember() {
		Room room = room("11111111-1111-1111-1111-111111111111");
		when(roomAccessService.getRoomOrThrow(room.getPublicId())).thenReturn(room);
		when(roomAccessService.getMembershipOrThrow(room, USER_ID))
				.thenThrow(new BusinessException(ErrorCode.E403_FORBIDDEN, "방 접근 권한이 없습니다."));

		assertThatThrownBy(() -> roomCommandService.updateRoomPin(USER_ID, room.getPublicId(), true))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E403_FORBIDDEN));
	}

	@Test
	void leaveRoomShouldDeleteMembershipOnlyWhenMembersRemain() {
		Room room = room("11111111-1111-1111-1111-111111111111");
		RoomMember member = RoomMember.join(room, USER_ID);
		when(roomAccessService.getRoomForUpdateOrThrow(room.getPublicId())).thenReturn(room);
		when(roomMemberRepository.findByRoomAndUserId(room, USER_ID)).thenReturn(Optional.of(member));
		when(roomMemberRepository.countByRoomId(room.getId())).thenReturn(1L);

		roomCommandService.leaveRoom(USER_ID, room.getPublicId());

		verify(roomMemberRepository).delete(member);
		verify(roomRepository, never()).delete(room);
		verify(roomLinkRepository, never()).deleteByRoomId(room.getId());
	}

	@Test
	void leaveRoomShouldDeleteRoomAndRoomLinksWhenLastMember() {
		Room room = room("11111111-1111-1111-1111-111111111111");
		RoomMember member = RoomMember.join(room, USER_ID);
		when(roomAccessService.getRoomForUpdateOrThrow(room.getPublicId())).thenReturn(room);
		when(roomMemberRepository.findByRoomAndUserId(room, USER_ID)).thenReturn(Optional.of(member));
		when(roomMemberRepository.countByRoomId(room.getId())).thenReturn(0L);

		roomCommandService.leaveRoom(USER_ID, room.getPublicId());

		verify(roomLinkRepository).deleteByRoomId(room.getId());
		verify(roomRepository).delete(room);
	}

	@Test
	void leaveRoomShouldThrowForbiddenWhenUserIsNotMember() {
		Room room = room("11111111-1111-1111-1111-111111111111");
		when(roomAccessService.getRoomForUpdateOrThrow(room.getPublicId())).thenReturn(room);
		when(roomMemberRepository.findByRoomAndUserId(room, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> roomCommandService.leaveRoom(USER_ID, room.getPublicId()))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E403_FORBIDDEN));
	}

	private static Room room(String publicId) {
		Room room = Room.create(publicId, "Test Room", "INVITE123456", USER_ID);
		ReflectionTestUtils.setField(room, "id", 1L);
		ReflectionTestUtils.setField(room, "createdAt", Instant.parse("2026-04-16T00:00:00Z"));
		return room;
	}
}
