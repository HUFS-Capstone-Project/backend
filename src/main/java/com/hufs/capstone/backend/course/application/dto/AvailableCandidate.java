package com.hufs.capstone.backend.course.application.dto;

import com.hufs.capstone.backend.place.domain.entity.RoomPlace;

public record AvailableCandidate(
		RoomPlace roomPlace,
		String businessHoursJson
) {
}
