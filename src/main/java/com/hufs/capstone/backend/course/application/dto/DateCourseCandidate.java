package com.hufs.capstone.backend.course.application.dto;

import com.hufs.capstone.backend.link.domain.LinkSourceType;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.math.BigDecimal;
import java.time.Instant;

public record DateCourseCandidate(
		RoomPlace roomPlace,
		String categoryCode,
		String tagCode,
		BigDecimal latitude,
		BigDecimal longitude,
		Instant createdAt,
		LinkSourceType linkSourceType,
		Long likeCount,
		boolean hasOriginLink,
		String businessHoursJson
) {
}
