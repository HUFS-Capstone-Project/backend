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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@RequestMapping("/api/v1/links")
@SecurityRequirement(name = "bearer-jwt")
public interface LinkApi {

	@Operation(
			tags = {"Link"},
			summary = "링크 등록 API",
			description = "링크를 등록하고 processing 서버에 job 생성을 요청합니다. 성공 응답은 CommonResponse, 실패 응답은 ProblemDetail 형식을 사용합니다."
	)
	@ApiResponse(responseCode = "201", description = "Created")
	@PostMapping
	ResponseEntity<CommonResponse<RegisterLinkResponse>> register(
			@Valid @RequestBody RegisterLinkRequest request,
			@Parameter(description = "CSRF token header value (XSRF-TOKEN cookie value)")
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);

	@Operation(
			tags = {"Link"},
			summary = "링크 상태 조회 API",
			description = "링크 처리 상태를 조회하고 완료된 경우 caption을 함께 반환합니다. 성공 응답은 CommonResponse, 실패 응답은 ProblemDetail 형식을 사용합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/{linkId}")
	CommonResponse<LinkStatusResponse> getLink(@PathVariable Long linkId);
}
