package com.hufs.capstone.backend.auth.api.controller.swagger;

import com.hufs.capstone.backend.auth.api.request.WebExchangeTicketRequest;
import com.hufs.capstone.backend.auth.api.response.AuthTokenBootstrapResponse;
import com.hufs.capstone.backend.auth.api.response.TokenResponse;
import com.hufs.capstone.backend.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/auth")
public interface AuthWebApi {

	@Operation(
			tags = {"Auth Web"},
			summary = "웹 로그인 티켓 교환 API",
			description = "웹 로그인 티켓을 액세스 토큰으로 교환합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@PostMapping("/web/exchange-ticket")
	CommonResponse<AuthTokenBootstrapResponse> exchangeWebTicket(
			@Valid @RequestBody WebExchangeTicketRequest request
	);

	@Operation(
			tags = {"Auth Web"},
			summary = "웹 액세스 토큰 갱신 API",
			description = "쿠키 기반 리프레시 토큰으로 액세스 토큰을 갱신합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@PostMapping("/refresh")
	CommonResponse<TokenResponse> refresh(
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse,
			@Parameter(description = "CSRF 토큰 헤더 값(XSRF-TOKEN 쿠키 값과 동일)")
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);

	@Operation(
			tags = {"Auth Web"},
			summary = "웹 로그아웃 API",
			description = "현재 웹 세션을 로그아웃합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@PostMapping("/logout")
	CommonResponse<Void> logout(
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse,
			@Parameter(description = "CSRF 토큰 헤더 값(XSRF-TOKEN 쿠키 값과 동일)")
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);
}
