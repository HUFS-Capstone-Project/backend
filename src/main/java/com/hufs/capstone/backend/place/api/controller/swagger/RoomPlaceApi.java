package com.hufs.capstone.backend.place.api.controller.swagger;

import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.place.api.request.UpdateRoomPlaceMemoRequest;
import com.hufs.capstone.backend.place.api.response.RoomPlaceCursorPageResponse;
import com.hufs.capstone.backend.place.api.response.RoomPlaceMapResponse;
import com.hufs.capstone.backend.place.api.response.RoomPlaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/v1/rooms/{roomId}/places")
@SecurityRequirement(name = "bearer-jwt")
public interface RoomPlaceApi {

	@Operation(
			tags = {"Room place"},
			summary = "방 저장 장소 검색 API",
			description = "방에 저장된 장소를 필터 조건에 맞게 최신 등록순으로 페이지 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping
	CommonResponse<RoomPlaceCursorPageResponse> searchRoomPlaces(
			@PathVariable String roomId,
			@Parameter(description = "장소명, 주소, 카테고리, 태그, 메모에 매칭되는 검색어.")
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String categoryCode,
			@RequestParam(required = false) String tagCode,
			@RequestParam(required = false) String sidoCode,
			@RequestParam(required = false) String sigunguCode,
			@Parameter(description = "등록자 사용자 ID 필터.")
			@RequestParam(required = false) Long createdBy,
			@RequestParam(required = false) Integer limit,
			@RequestParam(required = false) String cursor
	);

	@Operation(
			tags = {"Room place"},
			summary = "방 저장 장소 지도 핀 조회 API",
			description = "현재 지도 영역(bounds) 안의 저장 장소 목록을 반환합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/map")
	CommonResponse<RoomPlaceMapResponse> findMapPlaces(
			@PathVariable String roomId,
			@RequestParam BigDecimal swLat,
			@RequestParam BigDecimal swLng,
			@RequestParam BigDecimal neLat,
			@RequestParam BigDecimal neLng,
			@RequestParam(required = false) Integer zoom,
			@RequestParam(required = false) Long createdBy
	);

	@Operation(
			tags = {"Room place"},
			summary = "방 저장 장소 상세 조회 API",
			description = "로컬 DB 캐시에서 방 저장 장소 상세 정보를 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/{roomPlaceId}")
	CommonResponse<RoomPlaceResponse> getRoomPlace(
			@PathVariable String roomId,
			@PathVariable Long roomPlaceId
	);

	@Operation(
			tags = {"Room place"},
			summary = "방 저장 장소 메모 수정 API",
			description = "방에 저장된 장소의 메모를 수정합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@PatchMapping("/{roomPlaceId}")
	CommonResponse<Void> updateMemo(
			@PathVariable String roomId,
			@PathVariable Long roomPlaceId,
			@Valid @RequestBody UpdateRoomPlaceMemoRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);

	@Operation(
			tags = {"Room place"},
			summary = "방 저장 장소 삭제 API",
			description = "방에서 저장된 장소를 삭제합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@DeleteMapping("/{roomPlaceId}")
	CommonResponse<Void> deleteRoomPlace(
			@PathVariable String roomId,
			@PathVariable Long roomPlaceId,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);
}
