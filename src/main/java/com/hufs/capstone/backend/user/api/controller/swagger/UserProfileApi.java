package com.hufs.capstone.backend.user.api.controller.swagger;

import com.hufs.capstone.backend.auth.security.AuthUserPrincipal;
import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.user.api.request.CompleteOnboardingRequest;
import com.hufs.capstone.backend.user.api.response.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/users/me")
@SecurityRequirement(name = "bearer-jwt")
public interface UserProfileApi {

	@Operation(
			tags = {"User profile"},
			summary = "내 프로필 조회 API",
			description = "현재 인증된 사용자의 프로필(닉네임, 온보딩 완료 여부 등)을 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping
	CommonResponse<UserProfileResponse> getProfile(
			@Parameter(hidden = true) @AuthenticationPrincipal AuthUserPrincipal principal
	);

	@Operation(
			tags = {"User profile"},
			summary = "온보딩 완료 API",
			description = "미완료 사용자에 한해 닉네임과 약관 동의 정보를 저장하고 온보딩을 완료합니다. 이미 완료된 경우 HTTP 409 Conflict를 반환합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@PostMapping("/onboarding")
	CommonResponse<UserProfileResponse> completeOnboarding(
			@Valid @RequestBody CompleteOnboardingRequest request,
			@Parameter(description = "CSRF 토큰 헤더 값(XSRF-TOKEN 쿠키 값)")
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthUserPrincipal principal
	);
}
