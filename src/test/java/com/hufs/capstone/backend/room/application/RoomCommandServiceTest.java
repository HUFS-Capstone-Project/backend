package com.hufs.capstone.backend.room.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.room.application.dto.CreateRoomResult;
import com.hufs.capstone.backend.room.application.dto.JoinRoomResult;
import com.hufs.capstone.backend.room.domain.RoomMemberRole;
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
	private RoomInviteCodeGenerator inviteCodeGenerator;

	@Mock
	private RoomJoinRateLimiter roomJoinRateLimiter;

	@InjectMocks
	private RoomCommandService roomCommandService;

	@Test
	void createRoomShouldCreateOwnerMembership() {
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
		assertThat(result.role()).isEqualTo(RoomMemberRole.OWNER);
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
		assertThat(result.role()).isEqualTo(RoomMemberRole.MEMBER);
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

	private static Room room(String publicId) {
		Room room = Room.create(publicId, "Test Room", "INVITE123456", USER_ID);
		ReflectionTestUtils.setField(room, "id", 1L);
		ReflectionTestUtils.setField(room, "createdAt", Instant.parse("2026-04-16T00:00:00Z"));
		return room;
	}
}

