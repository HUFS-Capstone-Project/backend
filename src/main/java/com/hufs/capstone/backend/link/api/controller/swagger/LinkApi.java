package com.hufs.capstone.backend.link.api.controller.swagger;

import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.link.api.request.AnalyzeLinkRequest;
import com.hufs.capstone.backend.link.api.response.LinkAnalysisRequestResponse;
import com.hufs.capstone.backend.link.api.response.LinkAnalysisResponse;
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

@RequestMapping("/api/v1/rooms/{roomId}/links")
@SecurityRequirement(name = "bearer-jwt")
public interface LinkApi {

	@Operation(
			tags = {"Link"},
			summary = "링크 분석 요청 API",
			description = "방 멤버십을 검증한 뒤 링크 분석을 요청합니다. 신규 분석 대상이면 processing job 생성을 요청합니다."
	)
	@ApiResponse(responseCode = "201", description = "Created")
	@ApiResponse(responseCode = "200", description = "OK")
	@PostMapping("/analyze")
	ResponseEntity<CommonResponse<LinkAnalysisRequestResponse>> analyzeLink(
			@PathVariable String roomId,
			@Valid @RequestBody AnalyzeLinkRequest request,
			@Parameter(description = "CSRF 토큰 헤더 값(XSRF-TOKEN 쿠키 값)")
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);

	@Operation(
			tags = {"Link"},
			summary = "링크 분석 상태 조회 API",
			description = "방 멤버십과 해당 방의 분석 요청 이력을 확인하고, 링크 분석 상태와 caption을 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/{linkId}/analysis")
	CommonResponse<LinkAnalysisResponse> getLinkAnalysis(@PathVariable String roomId, @PathVariable Long linkId);
}
