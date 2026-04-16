package com.hufs.capstone.backend.auth.api.controller.swagger;

import com.hufs.capstone.backend.auth.api.response.DevMasterTokenResponse;
import com.hufs.capstone.backend.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/v1/auth/dev")
public interface AuthDevApi {

	@Operation(
			tags = {"Auth Dev"},
			summary = "Swagger 테스트용 마스터 JWT 발급 API",
			description = "개발/스테이징 환경 전용입니다. userId를 전달하면 해당 사용자 토큰을 발급하고, 없으면 테스트 사용자를 생성한 뒤 토큰을 발급합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/master-token")
	CommonResponse<DevMasterTokenResponse> issueMasterToken(
			HttpServletRequest servletRequest,
			@RequestParam(name = "userId", required = false) Long userId
	);
}
