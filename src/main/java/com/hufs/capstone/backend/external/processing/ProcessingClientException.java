package com.hufs.capstone.backend.external.processing;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class ProcessingClientException extends RuntimeException {

	private final ProcessingClientErrorType errorType;
	private final HttpStatusCode status;
	private final String responseBody;
	private final String processingErrorCode;

	public ProcessingClientException(String message, HttpStatusCode status, String responseBody) {
		this(message, status, responseBody, classify(status), null, null);
	}

	public ProcessingClientException(
			String message,
			HttpStatusCode status,
			String responseBody,
			ProcessingClientErrorType errorType,
			String processingErrorCode,
			Throwable cause
	) {
		super(message, cause);
		this.errorType = errorType;
		this.status = status;
		this.responseBody = responseBody;
		this.processingErrorCode = processingErrorCode;
	}

	public boolean hasStatus(int statusCode) {
		return status != null && status.value() == statusCode;
	}

	private static ProcessingClientErrorType classify(HttpStatusCode status) {
		if (status == null) {
			return ProcessingClientErrorType.NETWORK;
		}
		if (status.is5xxServerError()) {
			return ProcessingClientErrorType.SERVER_ERROR;
		}
		return ProcessingClientErrorType.CLIENT_ERROR;
	}
}
