package com.hufs.capstone.backend.auth.api.request;

import jakarta.validation.constraints.NotBlank;

public record WebExchangeTicketRequest(
		@NotBlank(message = "로그인 티켓은 필수입니다.") String ticket
) {
}
