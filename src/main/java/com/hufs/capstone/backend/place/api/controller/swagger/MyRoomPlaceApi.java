package com.hufs.capstone.backend.place.api.controller.swagger;

import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.place.api.response.MyRoomPlaceCursorPageResponse;
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
			summary = "내 저장 장소 검색 API",
			description = "현재 사용자가 접근 가능한 방에 저장한 장소를 최신 등록순으로 페이지 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping
	CommonResponse<MyRoomPlaceCursorPageResponse> searchMyRoomPlaces(
			@Parameter(description = "장소명, 주소, 카테고리, 태그, 메모에 매칭되는 검색어.")
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String categoryCode,
			@RequestParam(required = false) String tagCode,
			@RequestParam(required = false) String sidoCode,
			@RequestParam(required = false) String sigunguCode,
			@RequestParam(required = false) Integer limit,
			@RequestParam(required = false) String cursor
	);
}
