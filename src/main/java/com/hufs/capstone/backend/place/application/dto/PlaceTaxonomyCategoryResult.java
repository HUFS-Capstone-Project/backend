package com.hufs.capstone.backend.place.application.dto;

import java.util.List;

public record PlaceTaxonomyCategoryResult(
		String code,
		String name,
		Integer sortOrder,
		List<PlaceTaxonomyTagGroupResult> tagGroups
) {
}
