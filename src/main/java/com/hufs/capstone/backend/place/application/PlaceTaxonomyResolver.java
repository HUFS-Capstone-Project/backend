package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.place.application.dto.ResolvedPlaceCategory;
import com.hufs.capstone.backend.place.application.dto.ResolvedPlaceTaxonomy;
import com.hufs.capstone.backend.place.domain.KakaoCategoryGroupPolicy;
import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;
import com.hufs.capstone.backend.place.domain.repository.PlaceCategoryRepository;
import com.hufs.capstone.backend.place.domain.repository.PlaceTagRepository;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaceTaxonomyResolver {

	private static final String SEPARATOR_PATTERN = "[\\s>/,·ㆍ|\\-_/()\\[\\]{}]+";

	private final PlaceCategoryRepository placeCategoryRepository;
	private final PlaceTagRepository placeTagRepository;

	public ResolvedPlaceTaxonomy resolve(String kakaoCategoryGroupCode, String kakaoCategoryName) {
		TaxonomyOverride override = resolveOverride(kakaoCategoryGroupCode, kakaoCategoryName);
		String categoryCode = resolveCategoryCode(kakaoCategoryGroupCode, override);
		PlaceCategory category = findCategory(categoryCode);
		List<PlaceTag> activeTags = findActiveTags(category);
		PlaceTag fallbackTag = activeTags.stream()
				.filter(tag -> KakaoCategoryGroupPolicy.FALLBACK_TAG_CODE.equals(tag.getCode()))
				.findFirst()
				.orElseThrow(() -> taxonomyConfigurationError("Missing fallback place tag: " + categoryCode + ".MISC"));
		PlaceTag matchedTag = override == null
				? matchTag(kakaoCategoryName, activeTags, fallbackTag)
				: findOverrideTag(activeTags, override);
		return new ResolvedPlaceTaxonomy(category, matchedTag);
	}

	public ResolvedPlaceCategory resolveCategory(String kakaoCategoryGroupCode, String kakaoCategoryName) {
		TaxonomyOverride override = resolveOverride(kakaoCategoryGroupCode, kakaoCategoryName);
		return ResolvedPlaceCategory.from(findCategory(resolveCategoryCode(kakaoCategoryGroupCode, override)));
	}

	private PlaceCategory findCategory(String categoryCode) {
		return placeCategoryRepository.findByCode(categoryCode)
				.orElseThrow(() -> taxonomyConfigurationError("Missing place category: " + categoryCode));
	}

	private List<PlaceTag> findActiveTags(PlaceCategory category) {
		return placeTagRepository.findActiveTaxonomyTags().stream()
				.filter(tag -> category.getId().equals(tag.getCategory().getId()))
				.toList();
	}

	private PlaceTag findOverrideTag(List<PlaceTag> activeTags, TaxonomyOverride override) {
		return activeTags.stream()
				.filter(tag -> override.tagCode().equals(tag.getCode()))
				.findFirst()
				.orElseThrow(() -> taxonomyConfigurationError(
						"Missing override place tag: " + override.categoryCode() + "." + override.tagCode()
				));
	}

	private PlaceTag matchTag(String kakaoCategoryName, List<PlaceTag> activeTags, PlaceTag fallbackTag) {
		String normalizedCategoryName = normalizeForMatch(kakaoCategoryName);
		if (normalizedCategoryName == null) {
			return fallbackTag;
		}
		return activeTags.stream()
				.filter(tag -> !KakaoCategoryGroupPolicy.FALLBACK_TAG_CODE.equals(tag.getCode()))
				.filter(tag -> {
					return normalizedTagNameCandidates(tag.getName()).stream()
							.anyMatch(normalizedCategoryName::contains);
				})
				.min(Comparator
						.comparingInt((PlaceTag tag) -> tag.getName() == null ? 0 : tag.getName().length())
						.reversed()
						.thenComparing(PlaceTag::getSortOrder, Comparator.nullsLast(Integer::compareTo))
						.thenComparing(PlaceTag::getId, Comparator.nullsLast(Long::compareTo)))
				.orElse(fallbackTag);
	}

	private static String normalizeForMatch(String value) {
		if (value == null) {
			return null;
		}
		String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFKC)
				.replaceAll(SEPARATOR_PATTERN, "")
				.trim();
		return normalized.isEmpty() ? null : normalized;
	}

	private static List<String> normalizedTagNameCandidates(String tagName) {
		String fullName = normalizeForMatch(tagName);
		if (fullName == null) {
			return List.of();
		}
		List<String> tokens = Arrays.stream(tagName.split("[,/·ㆍ|]+"))
				.map(PlaceTaxonomyResolver::normalizeForMatch)
				.filter(token -> token != null && !token.equals(fullName))
				.toList();
		if (tokens.isEmpty()) {
			return List.of(fullName);
		}
		return Stream.concat(Stream.of(fullName), tokens.stream())
				.toList();
	}

	private static TaxonomyOverride resolveOverride(String kakaoCategoryGroupCode, String kakaoCategoryName) {
		if (!KakaoCategoryGroupPolicy.KAKAO_CAFE.equals(normalizeCategoryGroupCode(kakaoCategoryGroupCode))) {
			return null;
		}
		String normalizedCategoryName = normalizeForMatch(kakaoCategoryName);
		if (normalizedCategoryName == null) {
			return null;
		}
		if (normalizedCategoryName.contains("보드카페")) {
			return new TaxonomyOverride(KakaoCategoryGroupPolicy.SERVICE_CATEGORY_ACTIVITY, "BOARD_GAME_CAFE");
		}
		if (normalizedCategoryName.contains("만화카페")) {
			return new TaxonomyOverride(KakaoCategoryGroupPolicy.SERVICE_CATEGORY_ACTIVITY, "COMIC_CAFE");
		}
		return null;
	}

	private static String resolveCategoryCode(String kakaoCategoryGroupCode, TaxonomyOverride override) {
		return override == null
				? KakaoCategoryGroupPolicy.resolveServiceCategoryCode(kakaoCategoryGroupCode)
				: override.categoryCode();
	}

	private static String normalizeCategoryGroupCode(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.ROOT);
	}

	private static BusinessException taxonomyConfigurationError(String message) {
		return new BusinessException(ErrorCode.E500_INTERNAL, message);
	}

	private record TaxonomyOverride(String categoryCode, String tagCode) {
	}
}
