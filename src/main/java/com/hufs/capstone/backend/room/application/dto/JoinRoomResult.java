package com.hufs.capstone.backend.room.application.dto;

import com.hufs.capstone.backend.room.domain.RoomMemberRole;
import java.time.Instant;

public record JoinRoomResult(
		String roomId,
		String roomName,
		RoomMemberRole role,
		Instant createdAt
) {
}

