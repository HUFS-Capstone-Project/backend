package com.hufs.capstone.backend.auth.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthPropertiesValidationTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void missingJwtAndRefreshSecretsViolateConstraints() {
		AuthProperties properties = new AuthProperties();

		Set<ConstraintViolation<AuthProperties>> violations = validator.validate(properties);

		assertThat(violations)
				.extracting(violation -> violation.getPropertyPath().toString())
				.contains("jwt.secretBase64", "refresh.hashSecret");
	}
}
