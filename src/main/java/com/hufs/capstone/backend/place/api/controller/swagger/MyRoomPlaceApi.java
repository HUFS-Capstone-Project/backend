package com.hufs.capstone.backend.place.api.controller.swagger;

import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.place.api.response.MyRoomPlacePageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/v1/users/me/places")
@SecurityRequirement(name = "bearer-jwt")
public interface MyRoomPlaceApi {

	@Operation(
			tags = {"My room place"},
			summary = "나의 장소 목록 조회 API",
			description = "현재 로그인한 사용자가 직접 저장했고, 현재 참여 중인 방에 속한 저장 장소 목록을 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping
	CommonResponse<MyRoomPlacePageResponse> searchMyRoomPlaces(
			@Parameter(description = "장소명, 주소, 카테고리명, 메모 검색어")
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String categoryCode,
			@RequestParam(required = false) String tagCode,
			@RequestParam(required = false) String sidoCode,
			@RequestParam(required = false) String sigunguCode,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) Integer limit
	);
}
