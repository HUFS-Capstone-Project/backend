package com.hufs.capstone.backend.link.application.dto;

import java.util.List;

public record SaveRoomPlacesCommand(
		List<String> kakaoPlaceIds
) {
}
