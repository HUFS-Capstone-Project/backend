package com.hufs.capstone.backend.course.api.request;

import com.hufs.capstone.backend.course.domain.DateCourseNamePolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DateCourseUpdateRequest(
		@NotBlank(message = "데이트 코스 이름은 필수입니다.")
		@Size(max = DateCourseNamePolicy.MAX_LENGTH, message = "데이트 코스 이름은 20자를 초과할 수 없습니다.")
		String courseName,

		@NotEmpty(message = "장소는 최소 1개 이상이어야 합니다.")
		List<@NotNull(message = "장소 ID는 null일 수 없습니다.") Long> roomPlaceIds
) {

	public DateCourseUpdateRequest {
		if (courseName != null) {
			courseName = courseName.trim();
		}
	}
}
