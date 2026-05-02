package com.hufs.capstone.backend.link.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveRoomPlacesRequest(
		@NotEmpty
		@Size(max = 20)
		List<@NotBlank @Size(max = 100) String> kakaoPlaceIds
) {
}
