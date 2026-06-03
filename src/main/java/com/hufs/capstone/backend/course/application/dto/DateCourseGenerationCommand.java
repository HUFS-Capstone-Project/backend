package com.hufs.capstone.backend.course.application.dto;

import java.time.Instant;
import java.util.List;

public record DateCourseGenerationCommand(
		String roomPublicId,
		List<CategorySlotCommand> categorySequence,
		Instant startDateTime,
		Instant endDateTime,
		String sigunguCode
) {
}
