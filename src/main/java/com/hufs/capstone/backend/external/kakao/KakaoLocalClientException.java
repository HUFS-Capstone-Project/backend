package com.hufs.capstone.backend.external.kakao;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class KakaoLocalClientException extends RuntimeException {

	private final HttpStatusCode status;
	private final String responseBody;

	public KakaoLocalClientException(String message, HttpStatusCode status, String responseBody, Throwable cause) {
		super(message, cause);
		this.status = status;
		this.responseBody = responseBody;
	}
}
