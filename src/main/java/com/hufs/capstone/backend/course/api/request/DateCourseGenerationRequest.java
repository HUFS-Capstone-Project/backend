package com.hufs.capstone.backend.course.api.request;

import com.hufs.capstone.backend.course.application.dto.DateCourseGenerationCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public record DateCourseGenerationRequest(
		@NotEmpty @Valid List<CategorySlotRequest> categorySequence,
		@NotNull Instant plannedDateTime,
		@NotBlank String sigunguCode
) {

	public DateCourseGenerationCommand toCommand(String roomPublicId) {
		return new DateCourseGenerationCommand(
				roomPublicId,
				categorySequence.stream().map(CategorySlotRequest::toCommand).toList(),
				plannedDateTime,
				sigunguCode
		);
	}
}
