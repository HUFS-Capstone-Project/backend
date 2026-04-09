package com.hufs.capstone.backend.auth.api.controller.swagger;

import com.hufs.capstone.backend.auth.api.request.WebExchangeTicketRequest;
import com.hufs.capstone.backend.auth.api.response.AuthTokenBootstrapResponse;
import com.hufs.capstone.backend.auth.api.response.TokenResponse;
import com.hufs.capstone.backend.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/auth")
public interface AuthWebApi {

	@Operation(
			tags = {"Auth Web"},
			summary = "웹 로그인 티켓 교환 API",
			description = "웹 로그인 티켓을 액세스 토큰으로 교환하는 API"
	)
	@ApiResponse(responseCode = "200", description = "성공")
	@PostMapping("/web/exchange-ticket")
	CommonResponse<AuthTokenBootstrapResponse> exchangeWebTicket(
			@Valid @RequestBody WebExchangeTicketRequest request
	);

	@Operation(
			tags = {"Auth Web"},
			summary = "액세스 토큰 재발급 API",
			description = "쿠키 기반 리프레시 토큰으로 액세스 토큰을 재발급하는 API"
	)
	@ApiResponse(responseCode = "200", description = "성공")
	@PostMapping("/refresh")
	CommonResponse<TokenResponse> refresh(
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse
	);

	@Operation(
			tags = {"Auth Web"},
			summary = "로그아웃 API",
			description = "웹 현재 세션을 로그아웃하는 API"
	)
	@ApiResponse(responseCode = "200", description = "성공")
	@PostMapping("/logout")
	CommonResponse<Void> logout(
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse
	);
}
