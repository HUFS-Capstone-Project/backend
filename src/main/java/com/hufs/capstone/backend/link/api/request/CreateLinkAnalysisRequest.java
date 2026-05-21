package com.hufs.capstone.backend.link.api.request;

import com.hufs.capstone.backend.link.domain.LinkSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLinkAnalysisRequest(
		@NotBlank @Size(max = 2048) String originalUrl,
		LinkSource source
) {
}
