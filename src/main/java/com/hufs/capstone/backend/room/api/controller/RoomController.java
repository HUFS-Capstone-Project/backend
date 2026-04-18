package com.hufs.capstone.backend.room.api.controller;

import com.hufs.capstone.backend.auth.security.SecurityUtils;
import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.room.api.controller.swagger.RoomApi;
import com.hufs.capstone.backend.room.api.request.CreateRoomRequest;
import com.hufs.capstone.backend.room.api.request.JoinRoomRequest;
import com.hufs.capstone.backend.room.api.request.UpdateRoomNameRequest;
import com.hufs.capstone.backend.room.api.request.UpdateRoomPinRequest;
import com.hufs.capstone.backend.room.api.response.CreateRoomResponse;
import com.hufs.capstone.backend.room.api.response.JoinRoomResponse;
import com.hufs.capstone.backend.room.api.response.RoomDetailResponse;
import com.hufs.capstone.backend.room.api.response.RoomSummaryResponse;
import com.hufs.capstone.backend.room.application.RoomCommandService;
import com.hufs.capstone.backend.room.application.RoomQueryService;
import com.hufs.capstone.backend.room.application.dto.CreateRoomResult;
import com.hufs.capstone.backend.room.application.dto.JoinRoomResult;
import com.hufs.capstone.backend.room.application.dto.RoomDetailResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class RoomController implements RoomApi {

	private final RoomCommandService roomCommandService;
	private final RoomQueryService roomQueryService;
	private final HttpServletRequest servletRequest;

	@Override
	public ResponseEntity<CommonResponse<CreateRoomResponse>> createRoom(
			@Valid @RequestBody CreateRoomRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		CreateRoomResult result = roomCommandService.createRoom(userId, request.name());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{roomId}")
				.buildAndExpand(result.roomId())
				.toUri();
		return ResponseEntity.created(location).body(CommonResponse.ok(CreateRoomResponse.from(result)));
	}

	@Override
	public CommonResponse<List<RoomSummaryResponse>> getMyRooms() {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		List<RoomSummaryResponse> response = roomQueryService.getMyRooms(userId).stream()
				.map(RoomSummaryResponse::from)
				.toList();
		return CommonResponse.ok(response);
	}

	@Override
	public CommonResponse<RoomDetailResponse> getRoom(@PathVariable String roomId) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		RoomDetailResult result = roomQueryService.getRoomDetail(userId, roomId);
		return CommonResponse.ok(RoomDetailResponse.from(result));
	}

	@Override
	public ResponseEntity<CommonResponse<JoinRoomResponse>> joinRoom(
			@Valid @RequestBody JoinRoomRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		String ipAddress = extractClientIp(servletRequest);
		JoinRoomResult result = roomCommandService.joinByInviteCode(userId, request.inviteCode(), ipAddress);
		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/v1/rooms/{roomId}")
				.buildAndExpand(result.roomId())
				.toUri();
		return ResponseEntity.created(location).body(CommonResponse.ok(JoinRoomResponse.from(result)));
	}

	@Override
	public CommonResponse<Void> renameRoom(
			@PathVariable String roomId,
			@Valid @RequestBody UpdateRoomNameRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		roomCommandService.renameRoom(userId, roomId, request.name());
		return CommonResponse.okMessage("방 이름이 변경되었습니다.");
	}

	@Override
	public CommonResponse<Void> updatePin(
			@PathVariable String roomId,
			@Valid @RequestBody UpdateRoomPinRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		roomCommandService.updateRoomPin(userId, roomId, request.pinned());
		return CommonResponse.okMessage(request.pinned() ? "방을 상단 고정했습니다." : "방 상단 고정을 해제했습니다.");
	}

	@Override
	public CommonResponse<Void> leaveRoom(
			@PathVariable String roomId,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		roomCommandService.leaveRoom(userId, roomId);
		return CommonResponse.okMessage("방에서 나갔습니다.");
	}

	private static String extractClientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}

