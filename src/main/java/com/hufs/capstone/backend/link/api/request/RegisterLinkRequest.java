package com.hufs.capstone.backend.link.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterLinkRequest(
		@NotBlank @Size(max = 2048) String url,
		@NotBlank @Size(max = 100) String roomId,
		@Size(max = 100) String source
) {
}
