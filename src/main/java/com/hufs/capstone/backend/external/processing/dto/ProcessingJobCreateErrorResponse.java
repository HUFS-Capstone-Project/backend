package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessingJobCreateErrorResponse(
		@JsonProperty("detail")
		Detail detail
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Detail(
			@JsonProperty("code")
			String code,
			@JsonProperty("message")
			String message,
			@JsonProperty("retryable")
			Boolean retryable,
			@JsonProperty("cooldown_seconds")
			Integer cooldownSeconds
	) {
	}
}
