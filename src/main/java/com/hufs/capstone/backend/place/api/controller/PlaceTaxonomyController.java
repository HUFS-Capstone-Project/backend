package com.hufs.capstone.backend.place.api.controller;

import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.place.api.controller.swagger.PlaceTaxonomyApi;
import com.hufs.capstone.backend.place.api.response.PlaceTaxonomyResponse;
import com.hufs.capstone.backend.place.application.PlaceTaxonomyQueryService;
import com.hufs.capstone.backend.place.application.dto.PlaceTaxonomyResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PlaceTaxonomyController implements PlaceTaxonomyApi {

	private final PlaceTaxonomyQueryService placeTaxonomyQueryService;

	@Override
	public CommonResponse<PlaceTaxonomyResponse> getPlaceTaxonomy() {
		PlaceTaxonomyResult result = placeTaxonomyQueryService.getPlaceTaxonomy();
		return CommonResponse.ok(PlaceTaxonomyResponse.from(result));
	}
}
