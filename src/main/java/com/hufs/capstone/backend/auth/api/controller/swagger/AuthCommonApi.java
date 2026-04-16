package com.hufs.capstone.backend.auth.api.controller.swagger;

import com.hufs.capstone.backend.auth.api.response.MeResponse;
import com.hufs.capstone.backend.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/auth")
public interface AuthCommonApi {

	@Operation(
			tags = {"Auth Common"},
			summary = "CSRF 쿠키 초기화 API",
			description = "응답 본문 없이 CSRF 쿠키를 발급하거나 갱신합니다."
	)
	@ApiResponse(responseCode = "204", description = "No Content")
	@GetMapping("/csrf")
	ResponseEntity<Void> csrf();

	@Operation(
			tags = {"Auth Common"},
			summary = "내 정보 조회 API",
			description = "현재 인증된 사용자의 기본 정보를 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/me")
	CommonResponse<MeResponse> me();

	@Operation(
			tags = {"Auth Common"},
			summary = "전체 기기 로그아웃 API",
			description = "현재 계정의 모든 활성 세션을 로그아웃 처리합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@PostMapping("/logout-all")
	CommonResponse<Void> logoutAll();
}
