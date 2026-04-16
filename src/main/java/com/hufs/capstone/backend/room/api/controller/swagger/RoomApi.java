package com.hufs.capstone.backend.room.api.controller.swagger;

import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.room.api.request.CreateRoomRequest;
import com.hufs.capstone.backend.room.api.request.JoinRoomRequest;
import com.hufs.capstone.backend.room.api.response.CreateRoomResponse;
import com.hufs.capstone.backend.room.api.response.JoinRoomResponse;
import com.hufs.capstone.backend.room.api.response.RoomDetailResponse;
import com.hufs.capstone.backend.room.api.response.RoomSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/rooms")
@SecurityRequirement(name = "bearer-jwt")
public interface RoomApi {

	@Operation(
			tags = {"Room"},
			summary = "방 생성 API",
			description = "현재 로그인한 사용자를 OWNER로 포함해 방을 생성합니다."
	)
	@ApiResponse(responseCode = "201", description = "Created")
	@PostMapping
	ResponseEntity<CommonResponse<CreateRoomResponse>> createRoom(
			@Valid @RequestBody CreateRoomRequest request,
			@Parameter(description = "CSRF 토큰 헤더 값(XSRF-TOKEN 쿠키 값)")
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);

	@Operation(
			tags = {"Room"},
			summary = "내 방 목록 조회 API",
			description = "현재 로그인한 사용자가 참여 중인 방 목록을 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping
	CommonResponse<List<RoomSummaryResponse>> getMyRooms();

	@Operation(
			tags = {"Room"},
			summary = "방 상세 조회 API",
			description = "방 상세 정보와 내 역할 정보를 조회합니다. 초대코드는 방 멤버에게 반환합니다."
	)
	@ApiResponse(responseCode = "200", description = "OK")
	@GetMapping("/{roomId}")
	CommonResponse<RoomDetailResponse> getRoom(@PathVariable String roomId);

	@Operation(
			tags = {"Room"},
			summary = "초대코드로 방 참여 API",
			description = "초대코드로 방에 참여합니다. 요청에는 Rate Limit이 적용됩니다."
	)
	@ApiResponse(responseCode = "201", description = "Created")
	@PostMapping("/join")
	ResponseEntity<CommonResponse<JoinRoomResponse>> joinRoom(
			@Valid @RequestBody JoinRoomRequest request,
			@Parameter(description = "CSRF 토큰 헤더 값(XSRF-TOKEN 쿠키 값)")
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	);
}
