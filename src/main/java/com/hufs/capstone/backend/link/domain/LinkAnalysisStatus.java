package com.hufs.capstone.backend.link.domain;

import java.util.Locale;
import java.util.Set;

public enum LinkAnalysisStatus {

	REQUESTED,
	PROCESSING,
	SUCCEEDED,
	FAILED,
	DISPATCH_FAILED;

	private static final Set<String> REQUESTED_VALUES = Set.of("REQUESTED", "CREATED", "PENDING", "QUEUED");
	private static final Set<String> PROCESSING_VALUES = Set.of("PROCESSING", "RUNNING", "IN_PROGRESS", "STARTED");
	private static final Set<String> SUCCEEDED_VALUES = Set.of("SUCCEEDED", "SUCCESS", "COMPLETED", "DONE", "FINISHED");
	private static final Set<String> FAILED_VALUES = Set.of("FAILED", "FAILURE", "ERROR", "CANCELED", "CANCELLED", "ABORTED");

	public boolean isTerminal() {
		return this == SUCCEEDED || this == FAILED || this == DISPATCH_FAILED;
	}

	public static LinkAnalysisStatus fromProcessingStatus(String rawStatus) {
		if (rawStatus == null || rawStatus.isBlank()) {
			return PROCESSING;
		}

		String normalized = rawStatus.trim()
				.toUpperCase(Locale.ROOT)
				.replace('-', '_');

		if (REQUESTED_VALUES.contains(normalized)) {
			return REQUESTED;
		}
		if (PROCESSING_VALUES.contains(normalized)) {
			return PROCESSING;
		}
		if (SUCCEEDED_VALUES.contains(normalized)) {
			return SUCCEEDED;
		}
		if (FAILED_VALUES.contains(normalized)) {
			return FAILED;
		}
		return PROCESSING;
	}
}
