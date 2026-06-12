package com.hufs.capstone.backend.place.api.controller;

import com.hufs.capstone.backend.auth.security.SecurityUtils;
import com.hufs.capstone.backend.global.pagination.CursorPageResult;
import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.place.api.controller.swagger.MyRoomPlaceApi;
import com.hufs.capstone.backend.place.api.response.MyRoomPlaceCursorPageResponse;
import com.hufs.capstone.backend.place.application.RoomPlaceQueryService;
import com.hufs.capstone.backend.place.application.dto.MyRoomPlaceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MyRoomPlaceController implements MyRoomPlaceApi {

	private final RoomPlaceQueryService roomPlaceQueryService;

	@Override
	public CommonResponse<MyRoomPlaceCursorPageResponse> searchMyRoomPlaces(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String categoryCode,
			@RequestParam(required = false) String tagCode,
			@RequestParam(required = false) String sidoCode,
			@RequestParam(required = false) String sigunguCode,
			@RequestParam(required = false) Integer limit,
			@RequestParam(required = false) String cursor
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		CursorPageResult<MyRoomPlaceResult> result = roomPlaceQueryService.searchMyRoomPlaces(
				userId,
				keyword,
				resolveCategoryCode(category, categoryCode),
				tagCode,
				sidoCode,
				sigunguCode,
				limit,
				cursor
		);
		return CommonResponse.ok(MyRoomPlaceCursorPageResponse.from(result));
	}

	private static String resolveCategoryCode(String category, String categoryCode) {
		if (category != null && !category.isBlank()) {
			return category;
		}
		return categoryCode;
	}
}
