package com.hufs.capstone.backend.course.api.request;

import com.hufs.capstone.backend.course.application.dto.DateCourseGenerationCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record DateCourseGenerationRequest(
		@NotEmpty(message = "카테고리 순서는 필수입니다.")
		@Size(max = 5, message = "카테고리 순서는 최대 5개까지 가능합니다.")
		List<@NotNull(message = "카테고리 슬롯은 필수입니다.") @Valid CategorySlotRequest> categorySequence,
		@NotNull(message = "시작 일시는 필수입니다.") Instant startDateTime,
		@NotNull(message = "종료 일시는 필수입니다.") Instant endDateTime,
		@NotBlank(message = "시/군/구 코드는 필수입니다.") String sigunguCode
) {

	public DateCourseGenerationCommand toCommand(String roomPublicId) {
		return new DateCourseGenerationCommand(
				roomPublicId,
				categorySequence.stream().map(CategorySlotRequest::toCommand).toList(),
				startDateTime,
				endDateTime,
				sigunguCode
		);
	}
}
