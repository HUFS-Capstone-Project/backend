package com.hufs.capstone.backend.user.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CompleteOnboardingRequestValidationTest {

	private static Validator validator;

	@BeforeAll
	static void initValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void shouldTrimNicknameAndPassWhenLengthIsTenAfterTrim() {
		CompleteOnboardingRequest request = new CompleteOnboardingRequest(
				" 1234567890 ",
				true,
				true,
				false
		);

		Set<ConstraintViolation<CompleteOnboardingRequest>> violations = validator.validate(request);

		assertThat(request.nickname()).isEqualTo("1234567890");
		assertThat(violations).isEmpty();
	}

	@Test
	void shouldFailWhenNicknameIsBlank() {
		CompleteOnboardingRequest request = new CompleteOnboardingRequest(
				"   ",
				true,
				true,
				false
		);

		Set<ConstraintViolation<CompleteOnboardingRequest>> violations = validator.validate(request);

		assertThat(violations)
				.anySatisfy(v -> {
					assertThat(v.getPropertyPath().toString()).isEqualTo("nickname");
					assertThat(v.getMessage()).isEqualTo("닉네임은 필수입니다.");
				});
	}

	@Test
	void shouldFailWhenNicknameExceedsTenCharacters() {
		CompleteOnboardingRequest request = new CompleteOnboardingRequest(
				"12345678901",
				true,
				true,
				false
		);

		Set<ConstraintViolation<CompleteOnboardingRequest>> violations = validator.validate(request);

		assertThat(violations)
				.anySatisfy(v -> {
					assertThat(v.getPropertyPath().toString()).isEqualTo("nickname");
					assertThat(v.getMessage()).isEqualTo("닉네임은 최대 10자까지 가능합니다.");
				});
	}

	@Test
	void shouldFailWhenRequiredTermsAreNotAgreed() {
		CompleteOnboardingRequest request = new CompleteOnboardingRequest(
				"nickname",
				false,
				false,
				false
		);

		Set<ConstraintViolation<CompleteOnboardingRequest>> violations = validator.validate(request);

		assertThat(violations)
				.anySatisfy(v -> {
					assertThat(v.getPropertyPath().toString()).isEqualTo("serviceTermsAgreed");
					assertThat(v.getMessage()).isEqualTo("서비스 이용약관 동의는 필수입니다.");
				});
		assertThat(violations)
				.anySatisfy(v -> {
					assertThat(v.getPropertyPath().toString()).isEqualTo("privacyPolicyAgreed");
					assertThat(v.getMessage()).isEqualTo("개인정보 수집 및 이용 동의는 필수입니다.");
				});
	}
}
