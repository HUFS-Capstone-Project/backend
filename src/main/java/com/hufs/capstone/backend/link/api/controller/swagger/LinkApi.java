package com.hufs.capstone.backend.link.api.controller.swagger;

import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.link.api.request.RegisterLinkRequest;
import com.hufs.capstone.backend.link.api.response.LinkStatusResponse;
import com.hufs.capstone.backend.link.api.response.RegisterLinkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/links")
@SecurityRequirement(name = "bearer-jwt")
public interface LinkApi {

	@Operation(
			tags = {"Link"},
			summary = "링크 등록 API",
			description = "방 멤버십을 검증한 뒤 링크를 저장하고, 필요하면 processing job 생성을 요청합니다."
	)
	@ApiResponse(responseCode = "201", description = "Created")
	@PostMapping
	ResponseEntity<CommonResponse<RegisterLinkResponse>> register(
			@Valid @RequestBody RegisterLinkRequest request,
			@Parameter(description = "CSRF 토큰 헤더 값(XSRF-TOKEN 쿠키 값)")
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);

	@Operation(
			tags = {"Link"},
			summary = "링크 상태 조회 API",
			description = "링크 접근 권한을 확인하고 processing 상태를 조회합니다. 완료된 경우 caption을 포함해 반환합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/{linkId}")
	CommonResponse<LinkStatusResponse> getLink(@PathVariable Long linkId);
}
