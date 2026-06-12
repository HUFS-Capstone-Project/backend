package com.hufs.capstone.backend.place.api.controller;

import com.hufs.capstone.backend.auth.security.SecurityUtils;
import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.place.api.controller.swagger.RoomPlaceApi;
import com.hufs.capstone.backend.place.api.request.UpdateRoomPlaceMemoRequest;
import com.hufs.capstone.backend.place.api.response.RoomPlaceCursorPageResponse;
import com.hufs.capstone.backend.place.api.response.RoomPlaceMapResponse;
import com.hufs.capstone.backend.place.api.response.RoomPlaceResponse;
import com.hufs.capstone.backend.place.application.RoomPlaceManagementService;
import com.hufs.capstone.backend.place.application.RoomPlaceQueryService;
import com.hufs.capstone.backend.global.pagination.CursorPageResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceResult;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
	public CommonResponse<RoomPlaceCursorPageResponse> searchRoomPlaces(
			@PathVariable String roomId,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String categoryCode,
			@RequestParam(required = false) String tagCode,
			@RequestParam(required = false) String sidoCode,
			@RequestParam(required = false) String sigunguCode,
			@RequestParam(required = false) Long createdBy,
			@RequestParam(required = false) Integer limit,
			@RequestParam(required = false) String cursor
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		CursorPageResult<RoomPlaceResult> result = roomPlaceQueryService.searchRoomPlaces(
				userId,
				roomId,
				keyword,
				resolveCategoryCode(category, categoryCode),
				tagCode,
				sidoCode,
				sigunguCode,
				createdBy,
				limit,
				cursor
		);
		return CommonResponse.ok(RoomPlaceCursorPageResponse.from(result));
	}

	@Override
	public CommonResponse<RoomPlaceMapResponse> findMapPlaces(
			@PathVariable String roomId,
			@RequestParam BigDecimal swLat,
			@RequestParam BigDecimal swLng,
			@RequestParam BigDecimal neLat,
			@RequestParam BigDecimal neLng,
			@RequestParam(required = false) Integer zoom
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		return CommonResponse.ok(RoomPlaceMapResponse.from(
				roomPlaceQueryService.findMapPlaces(userId, roomId, swLat, swLng, neLat, neLng)
		));
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
