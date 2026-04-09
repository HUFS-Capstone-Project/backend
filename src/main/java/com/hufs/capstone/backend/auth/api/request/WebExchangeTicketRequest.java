package com.hufs.capstone.backend.auth.api.request;

import jakarta.validation.constraints.NotBlank;

public record WebExchangeTicketRequest(
		@NotBlank String ticket
) {
}
