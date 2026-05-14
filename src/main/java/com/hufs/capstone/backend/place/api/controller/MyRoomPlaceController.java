package com.hufs.capstone.backend.place.api.controller;

import com.hufs.capstone.backend.auth.security.SecurityUtils;
import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.place.api.controller.swagger.MyRoomPlaceApi;
import com.hufs.capstone.backend.place.api.response.MyRoomPlacePageResponse;
import com.hufs.capstone.backend.place.application.RoomPlaceQueryService;
import com.hufs.capstone.backend.place.application.dto.MyRoomPlacePageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MyRoomPlaceController implements MyRoomPlaceApi {

	private final RoomPlaceQueryService roomPlaceQueryService;

	@Override
	public CommonResponse<MyRoomPlacePageResponse> searchMyRoomPlaces(
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
		MyRoomPlacePageResult result = roomPlaceQueryService.searchMyRoomPlaces(
				userId,
				keyword,
				resolveCategoryCode(category, categoryCode),
				tagCode,
				sidoCode,
				sigunguCode,
				page,
				limit,
				size
		);
		return CommonResponse.ok(MyRoomPlacePageResponse.from(result));
	}

	private static String resolveCategoryCode(String category, String categoryCode) {
		if (category != null && !category.isBlank()) {
			return category;
		}
		return categoryCode;
	}
}
