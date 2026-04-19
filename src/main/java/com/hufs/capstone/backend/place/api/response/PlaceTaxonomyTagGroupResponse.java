package com.hufs.capstone.backend.place.api.response;

import com.hufs.capstone.backend.place.application.dto.PlaceTaxonomyTagGroupResult;
import com.hufs.capstone.backend.place.application.dto.PlaceTaxonomyTagResult;
import java.util.ArrayList;
import java.util.List;

public record PlaceTaxonomyTagGroupResponse(
		String code,
		String name,
		Integer sortOrder,
		List<PlaceTaxonomyTagResponse> tags
) {

	public static PlaceTaxonomyTagGroupResponse from(PlaceTaxonomyTagGroupResult result) {
		List<PlaceTaxonomyTagResponse> tags = new ArrayList<>(result.tags().size());
		for (PlaceTaxonomyTagResult tag : result.tags()) {
			tags.add(PlaceTaxonomyTagResponse.from(tag));
		}
		return new PlaceTaxonomyTagGroupResponse(result.code(), result.name(), result.sortOrder(), tags);
	}
}
