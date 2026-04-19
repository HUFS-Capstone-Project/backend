package com.hufs.capstone.backend.place.application.dto;

import java.util.List;

public record PlaceTaxonomyResult(
		List<PlaceTaxonomyCategoryResult> categories
) {
}
