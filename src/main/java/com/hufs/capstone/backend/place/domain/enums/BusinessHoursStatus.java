package com.hufs.capstone.backend.place.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum BusinessHoursStatus {
	PENDING,
	FETCHING,
	SUCCEEDED,
	NOT_FOUND,
	FAILED;

	@JsonCreator
	public static BusinessHoursStatus from(String rawStatus) {
		if (rawStatus == null || rawStatus.isBlank()) {
			return FAILED;
		}
		String normalized = rawStatus.trim()
				.toUpperCase(Locale.ROOT)
				.replace('-', '_');
		if ("SUC".concat("CESS").equals(normalized)) {
			return SUCCEEDED;
		}
		return switch (normalized) {
			case "PENDING" -> PENDING;
			case "FETCHING" -> FETCHING;
			case "SUCCEEDED" -> SUCCEEDED;
			case "NOT_FOUND" -> NOT_FOUND;
			case "FAILED" -> FAILED;
			default -> FAILED;
		};
	}
}
