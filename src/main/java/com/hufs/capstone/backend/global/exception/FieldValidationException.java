package com.hufs.capstone.backend.global.exception;

import com.hufs.capstone.backend.global.response.FieldErrorDetail;
import java.util.List;

public class FieldValidationException extends RuntimeException {

	private static final String DEFAULT_MESSAGE = "입력값을 확인해 주세요.";

	private final List<FieldErrorDetail> fieldErrors;

	public FieldValidationException(String field, String message) {
		this(List.of(FieldErrorDetail.of(field, message)));
	}

	public FieldValidationException(String field, String message, Object rejectedValue) {
		this(List.of(FieldErrorDetail.of(field, message, rejectedValue)));
	}

	public FieldValidationException(List<FieldErrorDetail> fieldErrors) {
		this(DEFAULT_MESSAGE, fieldErrors);
	}

	public FieldValidationException(String message, List<FieldErrorDetail> fieldErrors) {
		super(message == null || message.isBlank() ? DEFAULT_MESSAGE : message);
		this.fieldErrors = List.copyOf(fieldErrors);
	}

	public List<FieldErrorDetail> getFieldErrors() {
		return fieldErrors;
	}
}
