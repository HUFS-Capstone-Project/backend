package com.hufs.capstone.backend.auth.api.controller.swagger;

import com.hufs.capstone.backend.auth.api.request.LogoutRequest;
import com.hufs.capstone.backend.auth.api.request.MobileExchangeRequest;
import com.hufs.capstone.backend.auth.api.request.RefreshRequest;
import com.hufs.capstone.backend.auth.api.response.TokenResponse;
import com.hufs.capstone.backend.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/auth")
public interface AuthMobileApi {

	@Operation(
			tags = {"Auth Mobile"},
			summary = "모바일 인증 코드 교환 API",
			description = "모바일 인증 코드를 액세스/리프레시 토큰으로 교환합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@PostMapping("/mobile/exchange")
	CommonResponse<TokenResponse> exchangeMobile(
			@Valid @RequestBody MobileExchangeRequest request,
			HttpServletRequest servletRequest,
			@RequestHeader(name = "X-Client-Platform", required = false) String clientPlatform
	);

	@Operation(
			tags = {"Auth Mobile"},
			summary = "모바일 액세스 토큰 갱신 API",
			description = "요청 본문의 리프레시 토큰으로 액세스 토큰을 갱신합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@PostMapping("/mobile/refresh")
	CommonResponse<TokenResponse> mobileRefresh(
			@Valid @RequestBody RefreshRequest request,
			HttpServletRequest servletRequest,
			@RequestHeader(name = "X-Client-Platform", required = false) String clientPlatform
	);

	@Operation(
			tags = {"Auth Mobile"},
			summary = "모바일 로그아웃 API",
			description = "모바일 리프레시 토큰을 폐기하고 로그아웃합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@PostMapping("/mobile/logout")
	CommonResponse<Void> mobileLogout(@Valid @RequestBody LogoutRequest request);
}
