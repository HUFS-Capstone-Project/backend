package com.hufs.capstone.backend.external.kakao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kakao.local")
public record KakaoLocalProperties(
		@NotBlank String baseUrl,
		@NotBlank String restApiKey,
		@Positive int connectTimeoutMs,
		@Positive int readTimeoutMs
) {
}
