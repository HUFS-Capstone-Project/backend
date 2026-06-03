package com.hufs.capstone.backend.place.api.request;

import jakarta.validation.constraints.Size;

public record UpdateRoomPlaceMemoRequest(
		@Size(max = 500, message = "메모는 500자를 초과할 수 없습니다.")
		String memo
) {
}
