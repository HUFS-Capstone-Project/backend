package com.hufs.capstone.backend.room.api.request;

import jakarta.validation.constraints.NotNull;

public record UpdateRoomPinRequest(
		@NotNull Boolean pinned
) {
}
