package com.hufs.capstone.backend.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.hufs.capstone.backend.global.response.FieldErrorDetail;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void fieldValidationExceptionReturnsFieldErrors() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/rooms");
		FieldValidationException exception =
				new FieldValidationException("name", "방 이름은 필수입니다.");

		ResponseEntity<ProblemDetail> response = handler.handleFieldValidation(exception, request);

		assertThat(response.getStatusCode().value()).isEqualTo(400);
		ProblemDetail body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.getDetail()).isEqualTo("입력값을 확인해 주세요.");
		assertThat(body.getProperties()).containsEntry("code", "E400_VALIDATION");
		assertThat(body.getInstance().toString()).isEqualTo("/api/v1/rooms");

		Object fieldErrors = body.getProperties().get("fieldErrors");
		assertThat(fieldErrors).isInstanceOf(List.class);
		assertThat((List<?>) fieldErrors)
				.singleElement()
				.isEqualTo(FieldErrorDetail.of("name", "방 이름은 필수입니다."));
	}

	@Test
	void businessExceptionReturnsDetailWithoutFieldErrors() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/rooms/join");
		BusinessException exception = new BusinessException(ErrorCode.E409_CONFLICT, "이미 참여한 방입니다.");

		ResponseEntity<ProblemDetail> response = handler.handleBusiness(exception, request);

		assertThat(response.getStatusCode().value()).isEqualTo(409);
		ProblemDetail body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.getDetail()).isEqualTo("이미 참여한 방입니다.");
		assertThat(body.getProperties()).containsEntry("code", "E409_CONFLICT");
		assertThat(body.getProperties()).doesNotContainKey("fieldErrors");
	}
}
