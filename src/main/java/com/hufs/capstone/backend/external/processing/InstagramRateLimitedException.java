package com.hufs.capstone.backend.external.processing;

import org.springframework.http.HttpStatus;

public class InstagramRateLimitedException extends ProcessingClientException {

	private final boolean retryable;
	private final Integer cooldownSeconds;

	public InstagramRateLimitedException(String message, String responseBody, boolean retryable, Integer cooldownSeconds) {
		super(
				message,
				HttpStatus.TOO_MANY_REQUESTS,
				responseBody,
				ProcessingClientErrorType.CLIENT_ERROR,
				ProcessingErrorCodes.INSTAGRAM_RATE_LIMITED,
				null
		);
		this.retryable = retryable;
		this.cooldownSeconds = cooldownSeconds;
	}

	public boolean isRetryable() {
		return retryable;
	}

	public Integer getCooldownSeconds() {
		return cooldownSeconds;
	}
}
