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
	private static final String CAFE_COFFEE_DESSERT_TAG_CODE = "COFFEE_DESSERT";
	private static final String PHOTO_STUDIO_TAG_CODE = "PHOTO_STUDIO";
	private static final List<String> PHOTO_STUDIO_KEYWORDS = List.of(
			"사진",
			"사진관",
			"셀프사진",
			"포토",
			"포토스튜디오",
			"사진스튜디오"
	);
	private static final List<OverrideRule> OVERRIDE_RULES = List.of(
			new OverrideRule(
					KakaoCategoryGroupPolicy.KAKAO_FOOD,
					KakaoCategoryGroupPolicy.SERVICE_CATEGORY_CAFE,
					"BAKERY",
					List.of(),
					List.of("제과", "베이커리")
			),
			new OverrideRule(
					KakaoCategoryGroupPolicy.KAKAO_CAFE,
					KakaoCategoryGroupPolicy.SERVICE_CATEGORY_ACTIVITY,
					"BOARD_GAME_CAFE",
					List.of(),
					List.of("보드카페")
			),
			new OverrideRule(
					KakaoCategoryGroupPolicy.KAKAO_CAFE,
					KakaoCategoryGroupPolicy.SERVICE_CATEGORY_ACTIVITY,
					"COMIC_CAFE",
					List.of(),
					List.of("만화카페", "만화방")
			),
			new OverrideRule(
					KakaoCategoryGroupPolicy.KAKAO_CAFE,
					KakaoCategoryGroupPolicy.SERVICE_CATEGORY_CAFE,
					"BAKERY",
					List.of(),
					List.of("제과", "베이커리")
			),
			new OverrideRule(
					KakaoCategoryGroupPolicy.KAKAO_CAFE,
					KakaoCategoryGroupPolicy.SERVICE_CATEGORY_CAFE,
					CAFE_COFFEE_DESSERT_TAG_CODE,
					List.of("카페", "음식점카페"),
					List.of("커피전문점", "디저트카페")
			)
	);
	private static final List<FallbackTagRule> FALLBACK_TAG_RULES = List.of(
			new FallbackTagRule(
					KakaoCategoryGroupPolicy.SERVICE_CATEGORY_ACTIVITY,
					PHOTO_STUDIO_TAG_CODE,
					PHOTO_STUDIO_KEYWORDS
			)
	);

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
				.orElseThrow(() -> taxonomyConfigurationError("폴백 장소 태그가 없습니다: " + categoryCode + ".MISC"));
		PlaceTag matchedTag;
		if (override == null) {
			matchedTag = matchTag(kakaoCategoryName, activeTags, fallbackTag);
			matchedTag = applyFallbackTagRules(
					categoryCode,
					kakaoCategoryName,
					activeTags,
					fallbackTag,
					matchedTag
			);
		} else {
			matchedTag = findOverrideTag(activeTags, override);
		}
		return new ResolvedPlaceTaxonomy(category, matchedTag);
	}

	public ResolvedPlaceCategory resolveCategory(String kakaoCategoryGroupCode, String kakaoCategoryName) {
		TaxonomyOverride override = resolveOverride(kakaoCategoryGroupCode, kakaoCategoryName);
		return ResolvedPlaceCategory.from(findCategory(resolveCategoryCode(kakaoCategoryGroupCode, override)));
	}

	private PlaceCategory findCategory(String categoryCode) {
		return placeCategoryRepository.findByCode(categoryCode)
				.orElseThrow(() -> taxonomyConfigurationError("장소 카테고리가 없습니다: " + categoryCode));
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
						"오버라이드 장소 태그가 없습니다: " + override.categoryCode() + "." + override.tagCode()
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

	private PlaceTag applyFallbackTagRules(
			String categoryCode,
			String kakaoCategoryName,
			List<PlaceTag> activeTags,
			PlaceTag fallbackTag,
			PlaceTag matchedTag
	) {
		if (matchedTag != fallbackTag
				|| FALLBACK_TAG_RULES.stream().noneMatch(rule -> rule.supports(categoryCode))) {
			return matchedTag;
		}
		String normalizedCategoryName = normalizeForMatch(kakaoCategoryName);
		if (normalizedCategoryName == null) {
			return matchedTag;
		}
		for (FallbackTagRule rule : FALLBACK_TAG_RULES) {
			if (!rule.matches(categoryCode, normalizedCategoryName)) {
				continue;
			}
			PlaceTag fallbackMatch = findTagByCode(activeTags, rule.tagCode());
			if (fallbackMatch != null) {
				return fallbackMatch;
			}
		}
		return matchedTag;
	}

	private static PlaceTag findTagByCode(List<PlaceTag> activeTags, String tagCode) {
		return activeTags.stream()
				.filter(tag -> tagCode.equals(tag.getCode()))
				.findFirst()
				.orElse(null);
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
		String normalizedCategoryName = normalizeForMatch(kakaoCategoryName);
		if (normalizedCategoryName == null) {
			return null;
		}

		String normalizedCategoryGroupCode = normalizeCategoryGroupCode(kakaoCategoryGroupCode);
		for (OverrideRule rule : OVERRIDE_RULES) {
			if (rule.matches(normalizedCategoryGroupCode, normalizedCategoryName)) {
				return new TaxonomyOverride(rule.categoryCode(), rule.tagCode());
			}
		}
		return null;
	}

	private static boolean containsAny(String normalizedCategoryName, List<String> keywords) {
		return keywords.stream()
				.map(PlaceTaxonomyResolver::normalizeForMatch)
				.anyMatch(normalizedCategoryName::contains);
	}

	private static boolean equalsAny(String normalizedCategoryName, List<String> candidates) {
		return candidates.stream()
				.map(PlaceTaxonomyResolver::normalizeForMatch)
				.anyMatch(normalizedCategoryName::equals);
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

	private record OverrideRule(
			String kakaoCategoryGroupCode,
			String categoryCode,
			String tagCode,
			List<String> exactCategoryNames,
			List<String> keywordFragments
	) {

		private boolean matches(String kakaoCategoryGroupCode, String normalizedCategoryName) {
			return this.kakaoCategoryGroupCode.equals(kakaoCategoryGroupCode)
					&& (equalsAny(normalizedCategoryName, exactCategoryNames)
							|| containsAny(normalizedCategoryName, keywordFragments));
		}
	}

	private record FallbackTagRule(String categoryCode, String tagCode, List<String> keywords) {

		private boolean supports(String categoryCode) {
			return this.categoryCode.equals(categoryCode);
		}

		private boolean matches(String categoryCode, String normalizedCategoryName) {
			return supports(categoryCode) && containsAny(normalizedCategoryName, keywords);
		}
	}
}
