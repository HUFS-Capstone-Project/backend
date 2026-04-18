package com.hufs.capstone.backend.room.api.request;

import com.hufs.capstone.backend.room.domain.RoomNamePolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoomNameRequest(
		@NotBlank @Size(max = RoomNamePolicy.MAX_LENGTH) String name
) {
}
