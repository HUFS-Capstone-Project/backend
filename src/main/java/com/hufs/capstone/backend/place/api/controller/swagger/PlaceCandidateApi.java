package com.hufs.capstone.backend.place.api.controller.swagger;

import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.place.api.response.PlaceCandidateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/v1/rooms/{roomId}/place-candidates")
@SecurityRequirement(name = "bearer-jwt")
public interface PlaceCandidateApi {

	@Operation(
			tags = {"Place candidate"},
			summary = "카카오 외부 장소 후보 검색 API",
			description = "카카오 로컬 API로 검색하고, 해당 방 기준 선택 가능 여부 등을 반영해 후보 목록을 반환합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	// TODO: Consider moving this to GET /api/v1/rooms/{roomId}/place-candidates?provider=KAKAO&keyword=...
	@GetMapping("/external")
	CommonResponse<List<PlaceCandidateResponse>> searchExternalCandidates(
			@PathVariable String roomId,
			@RequestParam String keyword,
			@RequestParam(required = false) String region,
			@RequestParam(required = false) String categoryGroupCode,
			@RequestParam(required = false) Integer limit
	);
}
