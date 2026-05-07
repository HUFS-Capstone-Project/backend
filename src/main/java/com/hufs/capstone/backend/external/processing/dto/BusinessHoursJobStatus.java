package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum BusinessHoursJobStatus {
	QUEUED,
	PROCESSING,
	SUCCEEDED,
	FAILED;

	@JsonCreator
	public static BusinessHoursJobStatus from(String rawStatus) {
		if (rawStatus == null || rawStatus.isBlank()) {
			return PROCESSING;
		}
		String normalized = rawStatus.trim()
				.toUpperCase(Locale.ROOT)
				.replace('-', '_');
		if ("SUC".concat("CESS").equals(normalized)) {
			return SUCCEEDED;
		}
		return switch (normalized) {
			case "QUEUED" -> QUEUED;
			case "PROCESSING" -> PROCESSING;
			case "SUCCEEDED" -> SUCCEEDED;
			case "FAILED" -> FAILED;
			default -> PROCESSING;
		};
	}
}
