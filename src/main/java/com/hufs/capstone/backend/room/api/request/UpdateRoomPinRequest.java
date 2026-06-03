package com.hufs.capstone.backend.room.api.request;

import jakarta.validation.constraints.NotNull;

public record UpdateRoomPinRequest(
		@NotNull(message = "핀 상태는 필수입니다.") Boolean pinned
) {
}
