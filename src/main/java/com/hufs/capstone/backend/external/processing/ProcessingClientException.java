package com.hufs.capstone.backend.external.processing;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class ProcessingClientException extends RuntimeException {

	private final HttpStatusCode status;
	private final String responseBody;

	public ProcessingClientException(String message, HttpStatusCode status, String responseBody) {
		super(message);
		this.status = status;
		this.responseBody = responseBody;
	}
}
