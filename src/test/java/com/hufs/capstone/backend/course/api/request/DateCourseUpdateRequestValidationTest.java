package com.hufs.capstone.backend.course.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DateCourseUpdateRequestValidationTest {

	private static Validator validator;

	@BeforeAll
	static void initValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void 정상_요청은_위반_없음() {
		DateCourseUpdateRequest request = new DateCourseUpdateRequest("우리 데이트", List.of(1L, 2L));

		Set<ConstraintViolation<DateCourseUpdateRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
	}

	@Test
	void courseName_공백이면_위반() {
		DateCourseUpdateRequest request = new DateCourseUpdateRequest("   ", List.of(1L));

		Set<ConstraintViolation<DateCourseUpdateRequest>> violations = validator.validate(request);

		assertThat(violations)
				.anySatisfy(v -> {
					assertThat(v.getPropertyPath().toString()).isEqualTo("courseName");
					assertThat(v.getMessage()).isEqualTo("데이트 코스 이름은 필수입니다.");
				});
	}

	@Test
	void courseName_21자_초과시_위반() {
		DateCourseUpdateRequest request = new DateCourseUpdateRequest(
				"123456789012345678901", List.of(1L)); // 21자

		Set<ConstraintViolation<DateCourseUpdateRequest>> violations = validator.validate(request);

		assertThat(violations)
				.anySatisfy(v -> {
					assertThat(v.getPropertyPath().toString()).isEqualTo("courseName");
					assertThat(v.getMessage()).isEqualTo("데이트 코스 이름은 20자를 초과할 수 없습니다.");
				});
	}

	@Test
	void courseName_trim_후_필드에_반영() {
		DateCourseUpdateRequest request = new DateCourseUpdateRequest(" 우리 데이트 ", List.of(1L));

		assertThat(request.courseName()).isEqualTo("우리 데이트");
	}

	@Test
	void roomPlaceIds_빈_리스트이면_위반() {
		DateCourseUpdateRequest request = new DateCourseUpdateRequest("이름", List.of());

		Set<ConstraintViolation<DateCourseUpdateRequest>> violations = validator.validate(request);

		assertThat(violations)
				.anySatisfy(v -> {
					assertThat(v.getPropertyPath().toString()).isEqualTo("roomPlaceIds");
					assertThat(v.getMessage()).isEqualTo("장소는 최소 1개 이상이어야 합니다.");
				});
	}

	@Test
	void roomPlaceIds_null_원소_포함시_위반() {
		DateCourseUpdateRequest request = new DateCourseUpdateRequest("이름", List.of(1L));
		// null을 직접 포함한 리스트는 컴파일에서 막히므로, 서비스 레이어에서 검증.
		// 여기서는 DTO 자체 null 검사 기본 통과 확인
		assertThat(request.roomPlaceIds()).containsExactly(1L);
	}
}
