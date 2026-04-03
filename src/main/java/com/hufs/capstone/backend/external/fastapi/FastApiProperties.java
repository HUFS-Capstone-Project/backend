package com.hufs.capstone.backend.external.fastapi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.fastapi")
public record FastApiProperties(
		@NotBlank String baseUrl,
		@Positive int connectTimeoutMs,
		@Positive int readTimeoutMs
) {
}
