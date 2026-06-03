package com.hufs.capstone.backend.course.api.request;

import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import jakarta.validation.constraints.NotBlank;

public record CategorySlotRequest(
		@NotBlank(message = "카테고리 코드는 필수입니다.") String categoryCode,
		String tagCode
) {

	public CategorySlotCommand toCommand() {
		return new CategorySlotCommand(categoryCode, tagCode);
	}
}
