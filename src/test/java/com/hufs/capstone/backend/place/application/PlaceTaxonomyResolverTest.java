package com.hufs.capstone.backend.place.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.place.application.dto.ResolvedPlaceTaxonomy;
import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;
import com.hufs.capstone.backend.place.domain.repository.PlaceCategoryRepository;
import com.hufs.capstone.backend.place.domain.repository.PlaceTagRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaceTaxonomyResolverTest {

	@Mock
	private PlaceCategoryRepository placeCategoryRepository;

	@Mock
	private PlaceTagRepository placeTagRepository;

	private PlaceTaxonomyResolver resolver;
	private PlaceCategory food;
	private PlaceCategory cafe;
	private PlaceCategory activity;

	@BeforeEach
	void setUp() {
		resolver = new PlaceTaxonomyResolver(placeCategoryRepository, placeTagRepository);
		food = category(1L, "FOOD", "음식점", 1);
		cafe = category(2L, "CAFE", "카페", 2);
		activity = category(3L, "ACTIVITY", "놀거리", 3);
		lenient().when(placeCategoryRepository.findByCode("FOOD")).thenReturn(Optional.of(food));
		lenient().when(placeCategoryRepository.findByCode("CAFE")).thenReturn(Optional.of(cafe));
		lenient().when(placeCategoryRepository.findByCode("ACTIVITY")).thenReturn(Optional.of(activity));
	}

	@Test
	void shouldResolveFoodCategoryFromKakaoFoodGroup() {
		PlaceTag japanese = tag(10L, food, "JAPANESE", "일식", 1);
		PlaceTag misc = tag(11L, food, "MISC", "기타", 9);
		when(placeTagRepository.findActiveTaxonomyTags()).thenReturn(List.of(japanese, misc));

		ResolvedPlaceTaxonomy result = resolver.resolve("FD6", "음식점 > 일식 > 돈까스");

		assertThat(result.category().getCode()).isEqualTo("FOOD");
		assertThat(result.tag().getCode()).isEqualTo("JAPANESE");
	}

	@Test
	void shouldResolveCafeCategoryFromKakaoCafeGroup() {
		PlaceTag bakery = tag(20L, cafe, "BAKERY", "제과,베이커리", 1);
		PlaceTag coffeeDessert = tag(21L, cafe, "COFFEE_DESSERT", "커피·디저트", 2);
		PlaceTag misc = tag(22L, cafe, "MISC", "기타", 9);
		when(placeTagRepository.findActiveTaxonomyTags()).thenReturn(List.of(bakery, coffeeDessert, misc));

		ResolvedPlaceTaxonomy result = resolver.resolve("CE7", "음식점 > 카페 > 베이커리");

		assertThat(result.category().getCode()).isEqualTo("CAFE");
		assertThat(result.tag().getCode()).isEqualTo("BAKERY");
	}

	@Test
	void shouldResolveCafeBakeryTagFromEitherConfectioneryOrBakeryKeyword() {
		PlaceTag bakery = tag(20L, cafe, "BAKERY", "제과,베이커리", 1);
		PlaceTag coffeeDessert = tag(21L, cafe, "COFFEE_DESSERT", "커피·디저트", 2);
		PlaceTag misc = tag(22L, cafe, "MISC", "기타", 9);
		when(placeTagRepository.findActiveTaxonomyTags()).thenReturn(List.of(bakery, coffeeDessert, misc));

		ResolvedPlaceTaxonomy confectionery = resolver.resolve("CE7", "카페 > 제과");
		ResolvedPlaceTaxonomy bakeryResult = resolver.resolve("CE7", "카페 > 베이커리");

		assertThat(confectionery.tag().getCode()).isEqualTo("BAKERY");
		assertThat(bakeryResult.tag().getCode()).isEqualTo("BAKERY");
		assertThat(confectionery.tag().getName()).isEqualTo("제과,베이커리");
	}

	@Test
	void shouldOverrideKakaoFoodBakeryToCafeBakery() {
		PlaceTag bakery = tag(20L, cafe, "BAKERY", "제과,베이커리", 1);
		PlaceTag coffeeDessert = tag(21L, cafe, "COFFEE_DESSERT", "커피·디저트", 2);
		PlaceTag misc = tag(22L, cafe, "MISC", "기타", 9);
		when(placeTagRepository.findActiveTaxonomyTags()).thenReturn(List.of(bakery, coffeeDessert, misc));

		ResolvedPlaceTaxonomy result = resolver.resolve("FD6", "음식점 > 간식 > 제과,베이커리");

		assertThat(result.category().getCode()).isEqualTo("CAFE");
		assertThat(result.tag().getCode()).isEqualTo("BAKERY");
	}

	@Test
	void shouldResolvePlainCafeAndCoffeeShopToCoffeeDessert() {
		PlaceTag bakery = tag(20L, cafe, "BAKERY", "제과,베이커리", 1);
		PlaceTag coffeeDessert = tag(21L, cafe, "COFFEE_DESSERT", "커피·디저트", 2);
		PlaceTag misc = tag(22L, cafe, "MISC", "기타", 9);
		when(placeTagRepository.findActiveTaxonomyTags()).thenReturn(List.of(bakery, coffeeDessert, misc));

		ResolvedPlaceTaxonomy plainCafe = resolver.resolve("CE7", "음식점 > 카페");
		ResolvedPlaceTaxonomy coffeeShop = resolver.resolve("CE7", "음식점 > 카페 > 커피전문점");

		assertThat(plainCafe.tag().getCode()).isEqualTo("COFFEE_DESSERT");
		assertThat(coffeeShop.tag().getCode()).isEqualTo("COFFEE_DESSERT");
	}

	@Test
	void shouldFallbackCafeThemeCafeToMisc() {
		PlaceTag bakery = tag(20L, cafe, "BAKERY", "제과,베이커리", 1);
		PlaceTag coffeeDessert = tag(21L, cafe, "COFFEE_DESSERT", "커피·디저트", 2);
		PlaceTag misc = tag(22L, cafe, "MISC", "기타", 9);
		when(placeTagRepository.findActiveTaxonomyTags()).thenReturn(List.of(bakery, coffeeDessert, misc));

		ResolvedPlaceTaxonomy result = resolver.resolve("CE7", "음식점 > 카페 > 테마카페 > 고양이카페");

		assertThat(result.category().getCode()).isEqualTo("CAFE");
		assertThat(result.tag().getCode()).isEqualTo("MISC");
	}

	@Test
	void shouldOverrideKakaoCafeBoardCafeToActivityBoardGameCafe() {
		PlaceTag boardGameCafe = tag(30L, activity, "BOARD_GAME_CAFE", "BOARD_GAME_CAFE", 1);
		PlaceTag misc = tag(31L, activity, "MISC", "MISC", 9);
		when(placeTagRepository.findActiveTaxonomyTags()).thenReturn(List.of(boardGameCafe, misc));

		ResolvedPlaceTaxonomy result = resolver.resolve(
				"CE7",
				"가정,생활 > 여가시설 > 보드카페 > 레드버튼"
		);

		assertThat(result.category().getCode()).isEqualTo("ACTIVITY");
		assertThat(result.tag().getCode()).isEqualTo("BOARD_GAME_CAFE");
	}

	@Test
	void shouldOverrideKakaoCafeComicCafeToActivityComicCafe() {
		PlaceTag comicCafe = tag(30L, activity, "COMIC_CAFE", "COMIC_CAFE", 1);
		PlaceTag misc = tag(31L, activity, "MISC", "MISC", 9);
		when(placeTagRepository.findActiveTaxonomyTags()).thenReturn(List.of(comicCafe, misc));

		ResolvedPlaceTaxonomy result = resolver.resolve(
				"CE7",
				"가정,생활 > 여가시설 > 만화방 > 만화카페 > 놀이"
		);

		assertThat(result.category().getCode()).isEqualTo("ACTIVITY");
		assertThat(result.tag().getCode()).isEqualTo("COMIC_CAFE");
	}

	@Test
	void shouldResolveActivityPhotoCategoryToPhotoStudioWhenNoTagNameMatches() {
		PlaceTag photoStudio = tag(30L, activity, "PHOTO_STUDIO", "포토스튜디오", 1);
		PlaceTag misc = tag(31L, activity, "MISC", "기타", 9);
		when(placeTagRepository.findActiveTaxonomyTags()).thenReturn(List.of(photoStudio, misc));

		ResolvedPlaceTaxonomy result = resolver.resolve("CT1", "문화,예술 > 사진 > 즉석사진 > 인생네컷");

		assertThat(result.category().getCode()).isEqualTo("ACTIVITY");
		assertThat(result.tag().getCode()).isEqualTo("PHOTO_STUDIO");
	}

	@Test
	void shouldNotOverrideKakaoCafePhotoKeywordToActivityPhotoStudio() {
		PlaceTag bakery = tag(20L, cafe, "BAKERY", "제과,베이커리", 1);
		PlaceTag coffeeDessert = tag(21L, cafe, "COFFEE_DESSERT", "커피·디저트", 2);
		PlaceTag misc = tag(22L, cafe, "MISC", "기타", 9);
		when(placeTagRepository.findActiveTaxonomyTags()).thenReturn(List.of(bakery, coffeeDessert, misc));

		ResolvedPlaceTaxonomy result = resolver.resolve("CE7", "음식점 > 카페 > 사진카페");

		assertThat(result.category().getCode()).isEqualTo("CAFE");
		assertThat(result.tag().getCode()).isEqualTo("MISC");
	}

	@Test
	void shouldFallbackToActivityCategoryWhenGroupIsUnknownOrMissing() {
		PlaceTag park = tag(30L, activity, "PARK", "공원", 1);
		PlaceTag misc = tag(31L, activity, "MISC", "기타", 9);
		when(placeTagRepository.findActiveTaxonomyTags()).thenReturn(List.of(park, misc));

		ResolvedPlaceTaxonomy unknown = resolver.resolve("CT1", "여행 > 공원");
		ResolvedPlaceTaxonomy missing = resolver.resolve(null, "문화시설 > 전시관");

		assertThat(unknown.category().getCode()).isEqualTo("ACTIVITY");
		assertThat(unknown.tag().getCode()).isEqualTo("PARK");
		assertThat(missing.category().getCode()).isEqualTo("ACTIVITY");
		assertThat(missing.tag().getCode()).isEqualTo("MISC");
	}

	@Test
	void shouldNormalizeCategoryNameAndPreferLongerTagName() {
		PlaceTag boardGame = tag(40L, activity, "BOARD_GAME", "보드게임", 1);
		PlaceTag boardGameCafe = tag(41L, activity, "BOARD_GAME_CAFE", "보드게임카페", 2);
		PlaceTag misc = tag(42L, activity, "MISC", "기타", 9);
		when(placeTagRepository.findActiveTaxonomyTags()).thenReturn(List.of(boardGame, boardGameCafe, misc));

		ResolvedPlaceTaxonomy result = resolver.resolve(null, "놀거리 > 보드 게임-카페");

		assertThat(result.tag().getCode()).isEqualTo("BOARD_GAME_CAFE");
	}

	@Test
	void shouldFailClearlyWhenFallbackMiscTagIsMissing() {
		when(placeTagRepository.findActiveTaxonomyTags()).thenReturn(List.of(tag(50L, food, "KOREAN", "한식", 1)));

		assertThatThrownBy(() -> resolver.resolve("FD6", "음식점 > 한식"))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E500_INTERNAL));
	}

	private static PlaceCategory category(Long id, String code, String name, int sortOrder) {
		PlaceCategory category = PlaceCategory.create(code, name, sortOrder, true);
		ReflectionTestUtils.setField(category, "id", id);
		return category;
	}

	private static PlaceTag tag(Long id, PlaceCategory category, String code, String name, int sortOrder) {
		PlaceTag tag = PlaceTag.create(category, null, code, name, sortOrder, true);
		ReflectionTestUtils.setField(tag, "id", id);
		return tag;
	}
}
