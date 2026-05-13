package com.hufs.capstone.backend.room.api.response;

import com.hufs.capstone.backend.room.application.dto.RoomMemberProfileResult;
import java.time.Instant;

public record RoomMemberProfileResponse(
		Long userId,
		String nickname,
		String profileImageUrl,
		Instant joinedAt,
		boolean me
) {

	public static RoomMemberProfileResponse from(RoomMemberProfileResult result) {
		return new RoomMemberProfileResponse(
				result.userId(),
				result.nickname(),
				result.profileImageUrl(),
				result.joinedAt(),
				result.me()
		);
	}
}
