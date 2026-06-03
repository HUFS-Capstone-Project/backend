package com.hufs.capstone.backend.room.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RoomNameRequestValidationTest {

	private static Validator validator;

	@BeforeAll
	static void initValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void createRoomNameShouldTrimBeforeValidation() {
		CreateRoomRequest request = new CreateRoomRequest(" 12345678901234567890 ");

		Set<ConstraintViolation<CreateRoomRequest>> violations = validator.validate(request);

		assertThat(request.name()).isEqualTo("12345678901234567890");
		assertThat(violations).isEmpty();
	}

	@Test
	void createRoomNameShouldReturnKoreanMessageWhenBlank() {
		CreateRoomRequest request = new CreateRoomRequest("   ");

		Set<ConstraintViolation<CreateRoomRequest>> violations = validator.validate(request);

		assertThat(violations)
				.anySatisfy(v -> {
					assertThat(v.getPropertyPath().toString()).isEqualTo("name");
					assertThat(v.getMessage()).isEqualTo("방 이름은 필수입니다.");
				});
	}

	@Test
	void updateRoomNameShouldReturnKoreanMessageWhenTooLong() {
		UpdateRoomNameRequest request = new UpdateRoomNameRequest("123456789012345678901");

		Set<ConstraintViolation<UpdateRoomNameRequest>> violations = validator.validate(request);

		assertThat(violations)
				.anySatisfy(v -> {
					assertThat(v.getPropertyPath().toString()).isEqualTo("name");
					assertThat(v.getMessage()).isEqualTo("방 이름은 20자를 초과할 수 없습니다.");
				});
	}
}
