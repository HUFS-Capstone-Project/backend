package com.hufs.capstone.backend.place.api.response;

import com.hufs.capstone.backend.place.application.dto.PlaceTaxonomyTagResult;

public record PlaceTaxonomyTagResponse(
		String code,
		String name,
		Integer sortOrder
) {

	public static PlaceTaxonomyTagResponse from(PlaceTaxonomyTagResult result) {
		return new PlaceTaxonomyTagResponse(result.code(), result.name(), result.sortOrder());
	}
}
