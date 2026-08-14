package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PlaceTaxonomyCatalog(
		Map<String, CategoryDefinition> categoriesByCode,
		Map<String, List<TagDefinition>> tagsByCategoryCode
) {

	public PlaceTaxonomyCatalog {
		categoriesByCode = Map.copyOf(categoriesByCode);
		Map<String, List<TagDefinition>> immutableTags = new LinkedHashMap<>();
		tagsByCategoryCode.forEach((code, tags) -> immutableTags.put(code, List.copyOf(tags)));
		tagsByCategoryCode = Map.copyOf(immutableTags);
	}

	public static PlaceTaxonomyCatalog from(List<PlaceCategory> categories, List<PlaceTag> tags) {
		Map<String, CategoryDefinition> categoryDefinitions = new LinkedHashMap<>();
		for (PlaceCategory category : categories) {
			categoryDefinitions.put(category.getCode(), CategoryDefinition.from(category));
		}

		Map<String, List<TagDefinition>> tagDefinitions = new LinkedHashMap<>();
		for (PlaceTag tag : tags) {
			tagDefinitions.computeIfAbsent(tag.getCategory().getCode(), ignored -> new ArrayList<>())
					.add(TagDefinition.from(tag));
		}
		return new PlaceTaxonomyCatalog(categoryDefinitions, tagDefinitions);
	}

	public CategoryDefinition category(String code) {
		return categoriesByCode.get(code);
	}

	public List<TagDefinition> tags(String categoryCode) {
		return tagsByCategoryCode.getOrDefault(categoryCode, List.of());
	}

	public record CategoryDefinition(Long id, String code, String name, Integer sortOrder) {

		private static CategoryDefinition from(PlaceCategory category) {
			return new CategoryDefinition(
					category.getId(),
					category.getCode(),
					category.getName(),
					category.getSortOrder()
			);
		}
	}

	public record TagDefinition(Long id, String code, String name, Integer sortOrder) {

		private static TagDefinition from(PlaceTag tag) {
			return new TagDefinition(tag.getId(), tag.getCode(), tag.getName(), tag.getSortOrder());
		}
	}
}
