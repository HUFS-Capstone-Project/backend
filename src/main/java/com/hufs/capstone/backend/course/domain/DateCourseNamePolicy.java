package com.hufs.capstone.backend.course.domain;

import com.hufs.capstone.backend.global.exception.FieldValidationException;

public final class DateCourseNamePolicy {

	public static final int MAX_LENGTH = 20;

	private DateCourseNamePolicy() {
	}

	public static String normalizeAndValidate(String courseName) {
		if (courseName == null || courseName.isBlank()) {
			throw new FieldValidationException("courseName", "데이트 코스 이름은 필수입니다.");
		}
		String normalized = courseName.trim();
		if (normalized.length() > MAX_LENGTH) {
			throw new FieldValidationException("courseName", "데이트 코스 이름은 20자를 초과할 수 없습니다.", normalized);
		}
		return normalized;
	}
}
