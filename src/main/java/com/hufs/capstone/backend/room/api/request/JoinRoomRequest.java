package com.hufs.capstone.backend.room.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(
		@NotBlank @Size(max = 32) String inviteCode
) {
}

