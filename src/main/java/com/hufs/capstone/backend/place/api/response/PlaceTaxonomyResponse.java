package com.hufs.capstone.backend.place.api.response;

import com.hufs.capstone.backend.place.application.dto.PlaceTaxonomyCategoryResult;
import com.hufs.capstone.backend.place.application.dto.PlaceTaxonomyResult;
import java.util.ArrayList;
import java.util.List;

public record PlaceTaxonomyResponse(
		List<PlaceTaxonomyCategoryResponse> categories
) {

	public static PlaceTaxonomyResponse from(PlaceTaxonomyResult result) {
		List<PlaceTaxonomyCategoryResponse> categories = new ArrayList<>(result.categories().size());
		for (PlaceTaxonomyCategoryResult categoryResult : result.categories()) {
			categories.add(PlaceTaxonomyCategoryResponse.from(categoryResult));
		}
		return new PlaceTaxonomyResponse(categories);
	}
}
