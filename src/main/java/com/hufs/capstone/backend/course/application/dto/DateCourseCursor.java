package com.hufs.capstone.backend.course.application.dto;

import java.time.Instant;

public record DateCourseCursor(
		Instant savedAt,
		Long dateCoursePk
) {

	public DateCourseCursor {
		if (savedAt == null || dateCoursePk == null) {
			throw new IllegalArgumentException("Date course cursor values are required.");
		}
	}
}
