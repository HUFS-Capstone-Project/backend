package com.hufs.capstone.backend.course.api.request;

import com.hufs.capstone.backend.course.domain.DateCourseNamePolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DateCourseSaveRequest(
		@NotBlank(message = "데이트 코스 이름은 필수입니다.")
		@Size(max = DateCourseNamePolicy.MAX_LENGTH, message = "데이트 코스 이름은 20자를 초과할 수 없습니다.")
		String courseName
) {

	public DateCourseSaveRequest {
		if (courseName != null) {
			courseName = courseName.trim();
		}
	}
}
