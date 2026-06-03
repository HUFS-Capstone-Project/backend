package com.hufs.capstone.backend.room.api.request;

import com.hufs.capstone.backend.room.domain.RoomNamePolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
		@NotBlank(message = "방 이름은 필수입니다.")
		@Size(max = RoomNamePolicy.MAX_LENGTH, message = "방 이름은 20자를 초과할 수 없습니다.")
		String name
) {

	public CreateRoomRequest {
		if (name != null) {
			name = name.trim();
		}
	}
}

