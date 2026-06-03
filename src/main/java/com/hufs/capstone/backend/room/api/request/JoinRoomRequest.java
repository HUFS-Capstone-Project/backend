package com.hufs.capstone.backend.room.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(
		@NotBlank(message = "초대코드는 필수입니다.")
		@Size(max = 32, message = "초대코드는 32자를 초과할 수 없습니다.")
		String inviteCode
) {
}

