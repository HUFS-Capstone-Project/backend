package com.hufs.capstone.backend.course.api.request;

import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import jakarta.validation.constraints.NotBlank;

public record CategorySlotRequest(
		@NotBlank String categoryCode,
		String tagCode
) {

	public CategorySlotCommand toCommand() {
		return new CategorySlotCommand(categoryCode, tagCode);
	}
}
