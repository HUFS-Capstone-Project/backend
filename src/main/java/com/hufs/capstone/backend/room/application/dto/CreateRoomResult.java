package com.hufs.capstone.backend.room.application.dto;

import com.hufs.capstone.backend.room.domain.RoomMemberRole;
import java.time.Instant;

public record CreateRoomResult(
		String roomId,
		String roomName,
		String inviteCode,
		RoomMemberRole role,
		Instant createdAt
) {
}

