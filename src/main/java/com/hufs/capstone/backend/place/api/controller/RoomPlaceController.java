package com.hufs.capstone.backend.place.api.controller;

import com.hufs.capstone.backend.auth.security.SecurityUtils;
import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.place.api.controller.swagger.RoomPlaceApi;
import com.hufs.capstone.backend.place.api.request.UpdateRoomPlaceMemoRequest;
import com.hufs.capstone.backend.place.api.response.RoomPlacePageResponse;
import com.hufs.capstone.backend.place.api.response.RoomPlaceResponse;
import com.hufs.capstone.backend.place.application.RoomPlaceManagementService;
import com.hufs.capstone.backend.place.application.RoomPlaceQueryService;
import com.hufs.capstone.backend.place.application.dto.RoomPlacePageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoomPlaceController implements RoomPlaceApi {

	private final RoomPlaceQueryService roomPlaceQueryService;
	private final RoomPlaceManagementService roomPlaceManagementService;

	@Override
	public CommonResponse<RoomPlacePageResponse> searchRoomPlaces(
			@PathVariable String roomId,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String categoryCode,
			@RequestParam(required = false) String tagCode,
			@RequestParam(required = false) String sidoCode,
			@RequestParam(required = false) String sigunguCode,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) Integer limit
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		RoomPlacePageResult result = roomPlaceQueryService.searchRoomPlaces(
				userId,
				roomId,
				keyword,
				resolveCategoryCode(category, categoryCode),
				tagCode,
				sidoCode,
				sigunguCode,
				page,
				limit,
				size
		);
		return CommonResponse.ok(RoomPlacePageResponse.from(result));
	}

	@Override
	public CommonResponse<RoomPlaceResponse> getRoomPlace(
			@PathVariable String roomId,
			@PathVariable Long roomPlaceId
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		return CommonResponse.ok(RoomPlaceResponse.from(roomPlaceQueryService.getRoomPlace(userId, roomId, roomPlaceId)));
	}

	@Override
	public CommonResponse<Void> updateMemo(
			@PathVariable String roomId,
			@PathVariable Long roomPlaceId,
			@Valid @RequestBody UpdateRoomPlaceMemoRequest request,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		roomPlaceManagementService.updateMemo(userId, roomId, roomPlaceId, request.memo());
		return CommonResponse.okMessage("장소 메모가 변경되었습니다.");
	}

	@Override
	public CommonResponse<Void> deleteRoomPlace(
			@PathVariable String roomId,
			@PathVariable Long roomPlaceId,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		roomPlaceManagementService.deleteRoomPlace(userId, roomId, roomPlaceId);
		return CommonResponse.okMessage("장소가 삭제되었습니다.");
	}
	private static String resolveCategoryCode(String category, String categoryCode) {
		if (category != null && !category.isBlank()) {
			return category;
		}
		return categoryCode;
	}
}
