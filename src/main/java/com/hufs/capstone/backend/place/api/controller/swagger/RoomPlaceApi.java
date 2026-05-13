package com.hufs.capstone.backend.place.api.controller.swagger;

import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.place.api.request.UpdateRoomPlaceMemoRequest;
import com.hufs.capstone.backend.place.api.response.RoomPlacePageResponse;
import com.hufs.capstone.backend.place.api.response.RoomPlaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
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
			summary = "저장된 장소 목록 조회/검색 API",
			description = "해당 방에 저장된 장소 목록을 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping
	CommonResponse<RoomPlacePageResponse> searchRoomPlaces(
			@PathVariable String roomId,
			@Parameter(description = "저장된 장소 목록 검색어. 장소명, 주소, 카테고리명, 메모에서 부분 검색합니다.")
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String categoryCode,
			@RequestParam(required = false) String tagCode,
			@RequestParam(required = false) String sidoCode,
			@RequestParam(required = false) String sigunguCode,
			@Parameter(description = "저장자 userId. 지정하면 해당 방 멤버가 저장한 장소만 조회합니다.")
			@RequestParam(required = false) Long createdBy,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) Integer limit
	);

	@Operation(
			tags = {"Room place"},
			summary = "저장된 장소 상세 조회 API",
			description = "해당 방에 저장된 장소 상세를 DB 캐시 기준으로 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/{roomPlaceId}")
	CommonResponse<RoomPlaceResponse> getRoomPlace(
			@PathVariable String roomId,
			@PathVariable Long roomPlaceId
	);

	@Operation(
			tags = {"Room place"},
			summary = "저장된 장소 메모 수정 API",
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
			summary = "저장된 장소 삭제 API",
			description = "해당 방에 저장된 장소 연결을 삭제합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@DeleteMapping("/{roomPlaceId}")
	CommonResponse<Void> deleteRoomPlace(
			@PathVariable String roomId,
			@PathVariable Long roomPlaceId,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);
}
