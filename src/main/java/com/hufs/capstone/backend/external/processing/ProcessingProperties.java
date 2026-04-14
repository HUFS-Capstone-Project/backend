package com.hufs.capstone.backend.external.processing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "processing")
public record ProcessingProperties(
		@NotBlank String baseUrl,
		@NotBlank String internalApiKey,
		@Positive int connectTimeoutMs,
		@Positive int readTimeoutMs
) {
}
