package com.hufs.capstone.backend.user.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UpdateNicknameRequestValidationTest {

	private static Validator validator;

	@BeforeAll
	static void initValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void shouldTrimNicknameAndPassWhenLengthIsTenAfterTrim() {
		UpdateNicknameRequest request = new UpdateNicknameRequest(" 1234567890 ");

		Set<ConstraintViolation<UpdateNicknameRequest>> violations = validator.validate(request);

		assertThat(request.nickname()).isEqualTo("1234567890");
		assertThat(violations).isEmpty();
	}

	@Test
	void shouldFailWhenNicknameIsBlank() {
		UpdateNicknameRequest request = new UpdateNicknameRequest("   ");

		Set<ConstraintViolation<UpdateNicknameRequest>> violations = validator.validate(request);

		assertThat(violations)
				.anySatisfy(v -> {
					assertThat(v.getPropertyPath().toString()).isEqualTo("nickname");
					assertThat(v.getMessage()).isEqualTo("닉네임은 필수입니다.");
				});
	}

	@Test
	void shouldFailWhenNicknameExceedsTenCharacters() {
		UpdateNicknameRequest request = new UpdateNicknameRequest("12345678901");

		Set<ConstraintViolation<UpdateNicknameRequest>> violations = validator.validate(request);

		assertThat(violations)
				.anySatisfy(v -> {
					assertThat(v.getPropertyPath().toString()).isEqualTo("nickname");
					assertThat(v.getMessage()).isEqualTo("닉네임은 최대 10자까지 가능합니다.");
				});
	}
}
