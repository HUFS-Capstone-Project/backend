package com.hufs.capstone.backend.auth.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hufs.capstone.backend.global.exception.FieldValidationException;
import org.junit.jupiter.api.Test;

class PkceServiceTest {

	private final PkceService pkceService = new PkceService();

	@Test
	void verifyShouldRejectMissingCodeChallengeAsFieldValidation() {
		assertThatThrownBy(() -> pkceService.verify(null, "S256", "verifier"))
				.isInstanceOf(FieldValidationException.class)
				.satisfies(ex -> assertThat(((FieldValidationException) ex).getFieldErrors())
						.anySatisfy(error -> assertThat(error.field()).isEqualTo("codeChallenge")));
	}

	@Test
	void verifyShouldRejectMissingCodeVerifierAsFieldValidation() {
		assertThatThrownBy(() -> pkceService.verify("challenge", "S256", " "))
				.isInstanceOf(FieldValidationException.class)
				.satisfies(ex -> assertThat(((FieldValidationException) ex).getFieldErrors())
						.anySatisfy(error -> assertThat(error.field()).isEqualTo("codeVerifier")));
	}

	@Test
	void verifyShouldRejectUnsupportedMethodAsFieldValidation() {
		assertThatThrownBy(() -> pkceService.verify("challenge", "plain", "verifier"))
				.isInstanceOf(FieldValidationException.class)
				.satisfies(ex -> assertThat(((FieldValidationException) ex).getFieldErrors())
						.anySatisfy(error -> assertThat(error.field()).isEqualTo("codeChallengeMethod")));
	}
}
