package com.hufs.capstone.backend.course.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hufs.capstone.backend.global.exception.FieldValidationException;
import org.junit.jupiter.api.Test;

class DateCourseNamePolicyTest {

	@Test
	void normalizesTrimmedName() {
		assertThat(DateCourseNamePolicy.normalizeAndValidate("  강남 데이트  "))
				.isEqualTo("강남 데이트");
	}

	@Test
	void rejectsBlankName() {
		assertThatThrownBy(() -> DateCourseNamePolicy.normalizeAndValidate(" "))
				.isInstanceOf(FieldValidationException.class);
	}
}
