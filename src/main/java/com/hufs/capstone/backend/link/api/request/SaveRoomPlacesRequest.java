package com.hufs.capstone.backend.link.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveRoomPlacesRequest(
		@NotEmpty(message = "저장할 장소는 필수입니다.")
		@Size(max = 20, message = "저장할 장소는 최대 20개까지 가능합니다.")
		List<@NotBlank(message = "카카오 장소 ID는 필수입니다.")
		@Size(max = 100, message = "카카오 장소 ID는 100자를 초과할 수 없습니다.") String> kakaoPlaceIds
) {
}
