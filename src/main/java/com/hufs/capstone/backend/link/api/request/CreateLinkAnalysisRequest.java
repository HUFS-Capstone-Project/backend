package com.hufs.capstone.backend.link.api.request;

import com.hufs.capstone.backend.link.domain.LinkSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLinkAnalysisRequest(
		@NotBlank(message = "URL은 필수입니다.")
		@Size(max = 2048, message = "URL 길이가 너무 깁니다.")
		String originalUrl,
		LinkSource source
) {
}
