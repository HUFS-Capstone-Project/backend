package com.hufs.capstone.backend.place.api.controller.swagger;

import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.place.api.response.PlaceTaxonomyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/place-taxonomy")
@SecurityRequirement(name = "bearer-jwt")
public interface PlaceTaxonomyApi {

	@Operation(
			tags = {"Place taxonomy"},
			summary = "장소 카테고리 및 태그 필터 목록 조회 API",
			description = "지도 필터 패널 렌더링에 필요한 카테고리/태그 그룹/태그 기준 데이터를 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping
	CommonResponse<PlaceTaxonomyResponse> getPlaceTaxonomy();
}
