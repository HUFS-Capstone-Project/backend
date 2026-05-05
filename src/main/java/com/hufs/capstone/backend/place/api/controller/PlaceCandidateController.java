package com.hufs.capstone.backend.place.api.controller;

import com.hufs.capstone.backend.auth.security.SecurityUtils;
import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.place.api.controller.swagger.PlaceCandidateApi;
import com.hufs.capstone.backend.place.api.response.PlaceCandidateResponse;
import com.hufs.capstone.backend.place.application.PlaceCandidateQueryService;
import com.hufs.capstone.backend.place.application.dto.ExternalPlaceCandidateSearchQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PlaceCandidateController implements PlaceCandidateApi {

	private final PlaceCandidateQueryService placeCandidateQueryService;

	@Override
	public CommonResponse<List<PlaceCandidateResponse>> searchExternalCandidates(
			@PathVariable String roomId,
			@RequestParam String keyword,
			@RequestParam(required = false) String region,
			@RequestParam(required = false) String categoryGroupCode,
			@RequestParam(required = false) Integer limit
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		List<PlaceCandidateResponse> response = placeCandidateQueryService.searchExternalCandidates(
						userId,
						roomId,
						ExternalPlaceCandidateSearchQuery.of(keyword, region, categoryGroupCode, limit)
				).stream()
				.map(PlaceCandidateResponse::from)
				.toList();
		return CommonResponse.ok(response);
	}
}
