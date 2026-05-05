package com.hufs.capstone.backend.place.api.request;

import jakarta.validation.constraints.Size;

public record UpdateRoomPlaceMemoRequest(
		@Size(max = 500)
		String memo
) {
}
