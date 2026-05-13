package com.hufs.capstone.backend.place.application.dto;

import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;

public record ResolvedPlaceCategory(
		String code,
		String name
) {

	public static ResolvedPlaceCategory from(PlaceCategory category) {
		return new ResolvedPlaceCategory(category.getCode(), category.getName());
	}
}
