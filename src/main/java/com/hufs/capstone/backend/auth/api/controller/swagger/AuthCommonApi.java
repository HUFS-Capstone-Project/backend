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
			description = "토큰 본문을 반환하지 않고 CSRF 쿠키 발급·갱신을 처리합니다"
	)
	@ApiResponse(responseCode = "204", description = "No Content")
	@GetMapping("/csrf")
	ResponseEntity<Void> csrf();

	@Operation(
			tags = {"Auth Common"},
			summary = "현재 로그인 사용자 조회 API",
			description = "현재 인증된 사용자 정보를 조회하는 API"
	)
	@ApiResponse(responseCode = "200", description = "성공")
	@GetMapping("/me")
	CommonResponse<MeResponse> me();

	@Operation(
			tags = {"Auth Common"},
			summary = "모든 기기 로그아웃 API",
			description = "현재 사용자의 모든 세션을 로그아웃하는 API"
	)
	@ApiResponse(responseCode = "200", description = "성공")
	@PostMapping("/logout-all")
	CommonResponse<Void> logoutAll();
}
