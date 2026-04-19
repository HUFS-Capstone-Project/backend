package com.hufs.capstone.backend.place.application.dto;

import java.util.List;

public record PlaceTaxonomyTagGroupResult(
		String code,
		String name,
		Integer sortOrder,
		List<PlaceTaxonomyTagResult> tags
) {
}
