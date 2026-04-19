package com.hufs.capstone.backend.place.api.response;

import com.hufs.capstone.backend.place.application.dto.PlaceTaxonomyCategoryResult;
import com.hufs.capstone.backend.place.application.dto.PlaceTaxonomyTagGroupResult;
import java.util.ArrayList;
import java.util.List;

public record PlaceTaxonomyCategoryResponse(
		String code,
		String name,
		Integer sortOrder,
		List<PlaceTaxonomyTagGroupResponse> tagGroups
) {

	public static PlaceTaxonomyCategoryResponse from(PlaceTaxonomyCategoryResult result) {
		List<PlaceTaxonomyTagGroupResponse> tagGroups = new ArrayList<>(result.tagGroups().size());
		for (PlaceTaxonomyTagGroupResult groupResult : result.tagGroups()) {
			tagGroups.add(PlaceTaxonomyTagGroupResponse.from(groupResult));
		}

		return new PlaceTaxonomyCategoryResponse(
				result.code(),
				result.name(),
				result.sortOrder(),
				tagGroups
		);
	}
}
