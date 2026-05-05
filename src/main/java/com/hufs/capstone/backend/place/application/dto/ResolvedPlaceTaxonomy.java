package com.hufs.capstone.backend.place.application.dto;

import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;

public record ResolvedPlaceTaxonomy(
		PlaceCategory category,
		PlaceTag tag
) {
}
