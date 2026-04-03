package com.hufs.capstone.backend.global.response;

import com.hufs.capstone.backend.global.exception.ErrorCode;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

public final class ProblemDetailFactory {

	private ProblemDetailFactory() {
	}

	public static ProblemDetail create(ErrorCode errorCode, String message, List<FieldErrorDetail> fieldErrors, URI instance) {
		String detail = (message != null && !message.isBlank()) ? message : errorCode.getDefaultMessage();
		HttpStatusCode status = errorCode.getHttpStatus();
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
		pd.setTitle(errorCode.name());
		pd.setProperty("code", errorCode.name());
		pd.setProperty("timestamp", Instant.now());
		if (instance != null) {
			pd.setInstance(instance);
		}
		if (fieldErrors != null && !fieldErrors.isEmpty()) {
			pd.setProperty("fieldErrors", fieldErrors);
		}
		return pd;
	}

	public static ProblemDetail create(ErrorCode errorCode, String message, List<FieldErrorDetail> fieldErrors) {
		return create(errorCode, message, fieldErrors, null);
	}

	public static ProblemDetail create(ErrorCode errorCode, URI instance) {
		return create(errorCode, errorCode.getDefaultMessage(), null, instance);
	}

	public static ProblemDetail create(ErrorCode errorCode) {
		return create(errorCode, errorCode.getDefaultMessage(), null, null);
	}
}
