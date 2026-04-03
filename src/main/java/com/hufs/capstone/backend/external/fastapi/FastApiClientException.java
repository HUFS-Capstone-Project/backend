package com.hufs.capstone.backend.external.fastapi;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class FastApiClientException extends RuntimeException {

	private final HttpStatusCode status;
	private final String responseBody;

	public FastApiClientException(String message, HttpStatusCode status, String responseBody) {
		super(message);
		this.status = status;
		this.responseBody = responseBody;
	}
}
