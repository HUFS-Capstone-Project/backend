package com.hufs.capstone.backend.link.api.request;

import com.hufs.capstone.backend.link.domain.LinkSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterLinkRequest(
		@NotBlank @Size(max = 2048) String url,
		@NotBlank @Size(max = 36) String roomId,
		LinkSource source
) {
}
