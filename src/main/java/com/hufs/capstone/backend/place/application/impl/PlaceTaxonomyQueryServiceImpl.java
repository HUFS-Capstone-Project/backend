package com.hufs.capstone.backend.place.application.impl;

import com.hufs.capstone.backend.place.application.PlaceTaxonomyQueryService;
import com.hufs.capstone.backend.place.application.dto.PlaceTaxonomyCategoryResult;
import com.hufs.capstone.backend.place.application.dto.PlaceTaxonomyResult;
import com.hufs.capstone.backend.place.application.dto.PlaceTaxonomyTagGroupResult;
import com.hufs.capstone.backend.place.application.dto.PlaceTaxonomyTagResult;
import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;
import com.hufs.capstone.backend.place.domain.entity.PlaceTagGroup;
import com.hufs.capstone.backend.place.domain.repository.PlaceCategoryRepository;
import com.hufs.capstone.backend.place.domain.repository.PlaceTagRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceTaxonomyQueryServiceImpl implements PlaceTaxonomyQueryService {

	private static final String DEFAULT_GROUP_CODE = "DEFAULT";
	private static final int DEFAULT_GROUP_SORT_ORDER = Integer.MAX_VALUE;

	private final PlaceCategoryRepository placeCategoryRepository;
	private final PlaceTagRepository placeTagRepository;

	@Override
	@Transactional(readOnly = true)
	@Cacheable(cacheNames = "placeTaxonomy", key = "'all'")
	public PlaceTaxonomyResult getPlaceTaxonomy() {
		List<PlaceCategory> categories = placeCategoryRepository.findActiveCategories();
		List<PlaceTag> tags = placeTagRepository.findActiveTaxonomyTags();

		Map<Long, CategoryAccumulator> categoryAccumulators = createCategoryAccumulators(categories);
		appendTags(tags, categoryAccumulators);

		List<PlaceTaxonomyCategoryResult> categoryResults = new ArrayList<>(categories.size());
		for (PlaceCategory category : categories) {
			CategoryAccumulator accumulator = categoryAccumulators.get(category.getId());
			if (accumulator == null) {
				categoryResults.add(new PlaceTaxonomyCategoryResult(
						category.getCode(),
						category.getName(),
						category.getSortOrder(),
						List.of()
				));
				continue;
			}
			categoryResults.add(accumulator.toResult());
		}

		return new PlaceTaxonomyResult(List.copyOf(categoryResults));
	}

	private Map<Long, CategoryAccumulator> createCategoryAccumulators(List<PlaceCategory> categories) {
		Map<Long, CategoryAccumulator> accumulators = new LinkedHashMap<>();
		for (PlaceCategory category : categories) {
			accumulators.put(category.getId(), new CategoryAccumulator(category));
		}
		return accumulators;
	}

	private void appendTags(List<PlaceTag> tags, Map<Long, CategoryAccumulator> categoryAccumulators) {
		for (PlaceTag tag : tags) {
			CategoryAccumulator categoryAccumulator = categoryAccumulators.get(tag.getCategory().getId());
			if (categoryAccumulator == null) {
				continue;
			}

			PlaceTaxonomyTagResult tagResult = toTagResult(tag);
			PlaceTagGroup tagGroup = tag.getTagGroup();
			categoryAccumulator.addTag(tagGroup, tagResult);
		}
	}

	private PlaceTaxonomyTagResult toTagResult(PlaceTag tag) {
		return new PlaceTaxonomyTagResult(tag.getCode(), tag.getName(), tag.getSortOrder());
	}

	private static final class CategoryAccumulator {

		private final String code;
		private final String name;
		private final Integer sortOrder;
		private final Map<String, TagGroupAccumulator> groupedTags = new LinkedHashMap<>();

		private CategoryAccumulator(PlaceCategory category) {
			this.code = category.getCode();
			this.name = category.getName();
			this.sortOrder = category.getSortOrder();
		}

		private void addTag(PlaceTagGroup tagGroup, PlaceTaxonomyTagResult tag) {
			String groupKey = resolveGroupKey(tagGroup);
			TagGroupAccumulator groupAccumulator = groupedTags.computeIfAbsent(
					groupKey,
					key -> TagGroupAccumulator.from(tagGroup)
			);
			groupAccumulator.addTag(tag);
		}

		private PlaceTaxonomyCategoryResult toResult() {
			List<PlaceTaxonomyTagGroupResult> tagGroups = new ArrayList<>(groupedTags.size());
			for (TagGroupAccumulator groupAccumulator : groupedTags.values()) {
				tagGroups.add(groupAccumulator.toResult());
			}
			tagGroups.sort(Comparator
					.comparing(PlaceTaxonomyTagGroupResult::sortOrder, Comparator.nullsLast(Integer::compareTo))
					.thenComparing(PlaceTaxonomyTagGroupResult::code));
			return new PlaceTaxonomyCategoryResult(
					code,
					name,
					sortOrder,
					List.copyOf(tagGroups)
			);
		}

		private String resolveGroupKey(PlaceTagGroup tagGroup) {
			if (tagGroup == null) {
				return DEFAULT_GROUP_CODE;
			}
			return "GROUP:" + tagGroup.getId();
		}
	}

	private static final class TagGroupAccumulator {

		private final String code;
		private final String name;
		private final Integer sortOrder;
		private final List<PlaceTaxonomyTagResult> tags = new ArrayList<>();

		private TagGroupAccumulator(String code, String name, Integer sortOrder) {
			this.code = code;
			this.name = name;
			this.sortOrder = sortOrder;
		}

		private static TagGroupAccumulator from(PlaceTagGroup tagGroup) {
			if (tagGroup == null) {
				return new TagGroupAccumulator(DEFAULT_GROUP_CODE, null, DEFAULT_GROUP_SORT_ORDER);
			}
			return new TagGroupAccumulator(tagGroup.getCode(), tagGroup.getName(), tagGroup.getSortOrder());
		}

		private void addTag(PlaceTaxonomyTagResult tag) {
			tags.add(tag);
		}

		private PlaceTaxonomyTagGroupResult toResult() {
			List<PlaceTaxonomyTagResult> sortedTags = new ArrayList<>(tags);
			sortedTags.sort(Comparator
					.comparing(PlaceTaxonomyTagResult::sortOrder, Comparator.nullsLast(Integer::compareTo))
					.thenComparing(PlaceTaxonomyTagResult::code));
			return new PlaceTaxonomyTagGroupResult(code, name, sortOrder, List.copyOf(sortedTags));
		}
	}
}
