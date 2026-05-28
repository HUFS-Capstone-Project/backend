package com.hufs.capstone.backend.course.application.dto;

public record CategorySlotCommand(
		String categoryCode,
		String tagCode
) {

	public boolean isWildcard() {
		return tagCode == null || tagCode.isBlank();
	}
}
