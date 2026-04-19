package com.hufs.capstone.backend.place.infrastructure.seed;

import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;
import com.hufs.capstone.backend.place.domain.entity.PlaceTagGroup;
import com.hufs.capstone.backend.place.domain.repository.PlaceCategoryRepository;
import com.hufs.capstone.backend.place.domain.repository.PlaceTagGroupRepository;
import com.hufs.capstone.backend.place.domain.repository.PlaceTagRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PlaceTaxonomySeedDataInitializer implements ApplicationRunner {

	private static final String CATEGORY_FOOD = "FOOD";
	private static final String CATEGORY_CAFE = "CAFE";
	private static final String CATEGORY_ACTIVITY = "ACTIVITY";

	private static final String GROUP_EXPERIENCE = "EXPERIENCE";
	private static final String GROUP_CULTURE = "CULTURE";
	private static final String GROUP_REST = "REST";
	private static final String GROUP_MISC = "MISC";

	private final PlaceCategoryRepository placeCategoryRepository;
	private final PlaceTagGroupRepository placeTagGroupRepository;
	private final PlaceTagRepository placeTagRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		PlaceCategory foodCategory = upsertCategory(CATEGORY_FOOD, "맛집", 1);
		PlaceCategory cafeCategory = upsertCategory(CATEGORY_CAFE, "카페", 2);
		PlaceCategory activityCategory = upsertCategory(CATEGORY_ACTIVITY, "놀거리", 3);

		Map<String, PlaceTagGroup> activityGroups = upsertActivityGroups(activityCategory);

		seedFoodTags(foodCategory);
		seedCafeTags(cafeCategory);
		seedActivityTags(activityCategory, activityGroups);
	}

	private PlaceCategory upsertCategory(String code, String name, int sortOrder) {
		return placeCategoryRepository.findByCode(code)
				.map(category -> {
					category.updateMetadata(name, sortOrder, true);
					return category;
				})
				.orElseGet(() -> placeCategoryRepository.save(
						PlaceCategory.create(code, name, sortOrder, true)
				));
	}

	private Map<String, PlaceTagGroup> upsertActivityGroups(PlaceCategory activityCategory) {
		Map<String, PlaceTagGroup> groups = new LinkedHashMap<>();
		groups.put(GROUP_EXPERIENCE, upsertTagGroup(activityCategory, GROUP_EXPERIENCE, "체험", 1));
		groups.put(GROUP_CULTURE, upsertTagGroup(activityCategory, GROUP_CULTURE, "문화", 2));
		groups.put(GROUP_REST, upsertTagGroup(activityCategory, GROUP_REST, "휴식", 3));
		groups.put(GROUP_MISC, upsertTagGroup(activityCategory, GROUP_MISC, "기타", 4));
		return groups;
	}

	private PlaceTagGroup upsertTagGroup(
			PlaceCategory category,
			String code,
			String name,
			int sortOrder
	) {
		return placeTagGroupRepository.findByCategoryAndCode(category, code)
				.map(tagGroup -> {
					tagGroup.updateMetadata(name, sortOrder, true);
					return tagGroup;
				})
				.orElseGet(() -> placeTagGroupRepository.save(
						PlaceTagGroup.create(category, code, name, sortOrder, true)
				));
	}

	private void seedFoodTags(PlaceCategory category) {
		upsertTag(category, null, "KOREAN", "한식", 1);
		upsertTag(category, null, "CHINESE", "중식", 2);
		upsertTag(category, null, "JAPANESE", "일식", 3);
		upsertTag(category, null, "WESTERN", "양식", 4);
		upsertTag(category, null, "SNACK", "분식", 5);
		upsertTag(category, null, "ASIAN", "아시아식", 6);
		upsertTag(category, null, "BAR", "술집", 7);
		upsertTag(category, null, "MISC", "기타", 8);
	}

	private void seedCafeTags(PlaceCategory category) {
		upsertTag(category, null, "CONFECTIONERY", "제과", 1);
		upsertTag(category, null, "BAKERY", "베이커리", 2);
		upsertTag(category, null, "MISC", "기타", 3);
	}

	private void seedActivityTags(PlaceCategory category, Map<String, PlaceTagGroup> groups) {
		upsertTag(category, groups.get(GROUP_EXPERIENCE), "THEME_PARK", "테마파크", 1);
		upsertTag(category, groups.get(GROUP_EXPERIENCE), "BOARD_GAME_CAFE", "보드카페", 2);
		upsertTag(category, groups.get(GROUP_EXPERIENCE), "ESCAPE_ROOM_CAFE", "방탈출카페", 3);
		upsertTag(category, groups.get(GROUP_EXPERIENCE), "SPORTS", "스포츠", 4);

		upsertTag(category, groups.get(GROUP_CULTURE), "CULTURE_ART", "문화·예술", 1);
		upsertTag(category, groups.get(GROUP_CULTURE), "COMIC_CAFE", "만화카페", 2);

		upsertTag(category, groups.get(GROUP_REST), "PARK", "공원", 1);
		upsertTag(category, groups.get(GROUP_REST), "SAUNA", "찜질방", 2);
		upsertTag(category, groups.get(GROUP_REST), "AQUARIUM", "아쿠아리움", 3);

		upsertTag(category, groups.get(GROUP_MISC), "HOUSEHOLD_GOODS_STORE", "생활용품점", 1);
		upsertTag(category, groups.get(GROUP_MISC), "MISC", "기타", 2);
	}

	private PlaceTag upsertTag(
			PlaceCategory category,
			PlaceTagGroup group,
			String code,
			String name,
			int sortOrder
	) {
		return placeTagRepository.findByCategoryAndCode(category, code)
				.map(tag -> {
					tag.updateMetadata(group, name, sortOrder, true);
					return tag;
				})
				.orElseGet(() -> placeTagRepository.save(
						PlaceTag.create(category, group, code, name, sortOrder, true)
				));
	}
}
