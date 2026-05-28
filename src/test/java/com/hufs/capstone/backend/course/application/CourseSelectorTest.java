package com.hufs.capstone.backend.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.course.application.dto.AvailableCandidate;
import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.course.application.dto.CourseSelectionResult;
import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CourseSelectorTest {

	private final CourseScorer scorer = new CourseScorer();
	private final CourseSelector selector = new CourseSelector(scorer);

	@Test
	void firstSlot_noPrevious_distanceIgnored() {
		AvailableCandidate candidate = candidate(1L, "FOOD", "KOREAN", 37.0, 127.0, Instant.now());
		AvailablePool pool = new AvailablePool(List.of(candidate));
		List<CategorySlotCommand> slots = List.of(new CategorySlotCommand("FOOD", "KOREAN"));
		Set<Long> used = new HashSet<>();

		CourseSelectionResult result = selector.select(CourseMode.GENERAL, slots, pool, used, Instant.now());

		assertThat(result.pickedPlaces()).hasSize(1);
		assertThat(result.skippedSlotIndices()).isEmpty();
	}

	@Test
	void noMatchingCandidate_slotIsSkipped() {
		AvailableCandidate candidate = candidate(1L, "FOOD", "KOREAN", 37.0, 127.0, Instant.now());
		AvailablePool pool = new AvailablePool(List.of(candidate));
		// slot asks for CAFE but pool only has FOOD
		List<CategorySlotCommand> slots = List.of(new CategorySlotCommand("CAFE", "BAKERY"));
		Set<Long> used = new HashSet<>();

		CourseSelectionResult result = selector.select(CourseMode.GENERAL, slots, pool, used, Instant.now());

		assertThat(result.pickedPlaces()).isEmpty();
		assertThat(result.skippedSlotIndices()).containsExactly(0);
	}

	@Test
	void wildcardSlot_matchesAllTagsInCategory() {
		AvailableCandidate cafe1 = candidate(1L, "CAFE", "BAKERY", 37.0, 127.0, Instant.now());
		AvailableCandidate cafe2 = candidate(2L, "CAFE", "DESSERT", 37.01, 127.0, Instant.now());
		AvailablePool pool = new AvailablePool(List.of(cafe1, cafe2));
		List<CategorySlotCommand> slots = List.of(
				new CategorySlotCommand("CAFE", null),  // wildcard
				new CategorySlotCommand("CAFE", null)   // second wildcard slot
		);
		Set<Long> used = new HashSet<>();

		CourseSelectionResult result = selector.select(CourseMode.GENERAL, slots, pool, used, Instant.now());

		assertThat(result.pickedPlaces()).hasSize(2);
		assertThat(result.skippedSlotIndices()).isEmpty();
	}

	@Test
	void globallyUsedIds_preventCrossCourseDuplication() {
		AvailableCandidate candidate = candidate(1L, "FOOD", "KOREAN", 37.0, 127.0, Instant.now());
		AvailablePool pool = new AvailablePool(List.of(candidate));
		List<CategorySlotCommand> slots = List.of(new CategorySlotCommand("FOOD", "KOREAN"));

		Set<Long> used = new HashSet<>();
		// First course consumes id=1
		CourseSelectionResult first = selector.select(CourseMode.GENERAL, slots, pool, used, Instant.now());
		assertThat(first.pickedPlaces()).hasSize(1);
		assertThat(used).contains(1L);

		// Second course cannot reuse id=1
		CourseSelectionResult second = selector.select(CourseMode.TRENDY, slots, pool, used, Instant.now());
		assertThat(second.pickedPlaces()).isEmpty();
		assertThat(second.skippedSlotIndices()).containsExactly(0);
	}

	@Test
	void sameCourseDuplication_prevented() {
		AvailableCandidate candidate = candidate(1L, "FOOD", "KOREAN", 37.0, 127.0, Instant.now());
		AvailablePool pool = new AvailablePool(List.of(candidate));
		// Two identical slots — only one candidate available
		List<CategorySlotCommand> slots = List.of(
				new CategorySlotCommand("FOOD", "KOREAN"),
				new CategorySlotCommand("FOOD", "KOREAN")
		);
		Set<Long> used = new HashSet<>();

		CourseSelectionResult result = selector.select(CourseMode.GENERAL, slots, pool, used, Instant.now());

		assertThat(result.pickedPlaces()).hasSize(1);
		assertThat(result.skippedSlotIndices()).containsExactly(1);
	}

	@Test
	void popular_candidateWithoutLink_excluded() {
		AvailableCandidate withLink = candidate(1L, "FOOD", "KOREAN", 37.0, 127.0, Instant.now());
		AvailableCandidate noLink = candidateNoLink(2L, "FOOD", "KOREAN");

		AvailablePool pool = new AvailablePool(List.of(noLink, withLink));
		List<CategorySlotCommand> slots = List.of(new CategorySlotCommand("FOOD", "KOREAN"));
		Set<Long> used = new HashSet<>();

		CourseSelectionResult result = selector.select(CourseMode.POPULAR, slots, pool, used, Instant.now());

		assertThat(result.pickedPlaces()).hasSize(1);
		assertThat(result.pickedPlaces().get(0).getId()).isEqualTo(1L);
	}

	private static AvailableCandidate candidate(Long id, String catCode, String tagCode,
			double lat, double lng, Instant createdAt) {
		PlaceCategory category = mock(PlaceCategory.class);
		when(category.getCode()).thenReturn(catCode);
		PlaceTag tag = mock(PlaceTag.class);
		when(tag.getCode()).thenReturn(tagCode);
		Place place = mock(Place.class);
		when(place.getServiceCategory()).thenReturn(category);
		when(place.getServiceTag()).thenReturn(tag);
		when(place.getLatitude()).thenReturn(BigDecimal.valueOf(lat));
		when(place.getLongitude()).thenReturn(BigDecimal.valueOf(lng));
		RoomPlace roomPlace = mock(RoomPlace.class);
		when(roomPlace.getId()).thenReturn(id);
		when(roomPlace.getPlace()).thenReturn(place);
		when(roomPlace.getCreatedAt()).thenReturn(createdAt);
		when(roomPlace.getOriginRoomLink()).thenReturn(mock(com.hufs.capstone.backend.link.domain.entity.RoomLink.class));
		return new AvailableCandidate(roomPlace, null);
	}

	private static AvailableCandidate candidateNoLink(Long id, String catCode, String tagCode) {
		PlaceCategory category = mock(PlaceCategory.class);
		when(category.getCode()).thenReturn(catCode);
		PlaceTag tag = mock(PlaceTag.class);
		when(tag.getCode()).thenReturn(tagCode);
		Place place = mock(Place.class);
		when(place.getServiceCategory()).thenReturn(category);
		when(place.getServiceTag()).thenReturn(tag);
		when(place.getLatitude()).thenReturn(null);
		when(place.getLongitude()).thenReturn(null);
		RoomPlace roomPlace = mock(RoomPlace.class);
		when(roomPlace.getId()).thenReturn(id);
		when(roomPlace.getPlace()).thenReturn(place);
		when(roomPlace.getCreatedAt()).thenReturn(Instant.now());
		when(roomPlace.getOriginRoomLink()).thenReturn(null);
		return new AvailableCandidate(roomPlace, null);
	}
}
