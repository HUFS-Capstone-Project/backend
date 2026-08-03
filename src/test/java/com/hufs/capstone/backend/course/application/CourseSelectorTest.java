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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CourseSelectorTest {

	private static final long SEED = 42L;

	private final CourseScorer scorer = new CourseScorer();
	private final CourseSelector selector = new CourseSelector(scorer, new Random(SEED));

	@Test
	void firstSlotNoPreviousDistanceIgnored() {
		AvailableCandidate candidate = candidate(1L, "FOOD", "KOREAN", 37.0, 127.0, Instant.now());
		AvailablePool pool = new AvailablePool(List.of(candidate));
		List<CategorySlotCommand> slots = List.of(new CategorySlotCommand("FOOD", "KOREAN"));

		CourseSelectionResult result = selector.select(CourseMode.GENERAL, slots, pool, new HashMap<>(), Instant.now());

		assertThat(result.pickedPlaces()).hasSize(1);
		assertThat(result.skippedSlotIndices()).isEmpty();
	}

	@Test
	void noMatchingCandidateSlotIsSkipped() {
		AvailableCandidate candidate = candidate(1L, "FOOD", "KOREAN", 37.0, 127.0, Instant.now());
		AvailablePool pool = new AvailablePool(List.of(candidate));
		// slot asks for CAFE but pool only has FOOD
		List<CategorySlotCommand> slots = List.of(new CategorySlotCommand("CAFE", "BAKERY"));

		CourseSelectionResult result = selector.select(CourseMode.GENERAL, slots, pool, new HashMap<>(), Instant.now());

		assertThat(result.pickedPlaces()).isEmpty();
		assertThat(result.skippedSlotIndices()).containsExactly(0);
	}

	@Test
	void wildcardSlotMatchesAllTagsInCategory() {
		AvailableCandidate cafe1 = candidate(1L, "CAFE", "BAKERY", 37.0, 127.0, Instant.now());
		AvailableCandidate cafe2 = candidate(2L, "CAFE", "DESSERT", 37.01, 127.0, Instant.now());
		AvailablePool pool = new AvailablePool(List.of(cafe1, cafe2));
		List<CategorySlotCommand> slots = List.of(
				new CategorySlotCommand("CAFE", null),  // wildcard
				new CategorySlotCommand("CAFE", null)   // second wildcard slot
		);

		CourseSelectionResult result = selector.select(CourseMode.GENERAL, slots, pool, new HashMap<>(), Instant.now());

		assertThat(result.pickedPlaces()).hasSize(2);
		assertThat(result.skippedSlotIndices()).isEmpty();
	}

	@Test
	void crossCourseDuplicationAllowed() {
		// 소프트 룰: 코스 간 장소 중복은 허용된다(하드 제외 제거).
		AvailableCandidate candidate = candidate(1L, "FOOD", "KOREAN", 37.0, 127.0, Instant.now());
		AvailablePool pool = new AvailablePool(List.of(candidate));
		List<CategorySlotCommand> slots = List.of(new CategorySlotCommand("FOOD", "KOREAN"));

		// 첫 코스가 id=1을 사용했다고 가정하고 usageCount를 1로 둔다.
		Map<Long, Integer> usageCounts = new HashMap<>();
		usageCounts.put(1L, 1);

		// 두 번째 코스도 같은 장소를 다시 선택할 수 있어야 한다(제외되지 않음).
		CourseSelectionResult second = selector.select(CourseMode.TRENDY, slots, pool, usageCounts, Instant.now());

		assertThat(second.pickedPlaces()).hasSize(1);
		assertThat(second.pickedPlaces().get(0).getId()).isEqualTo(1L);
		assertThat(second.skippedSlotIndices()).isEmpty();
	}

	@Test
	void sameCourseDuplicationPrevented() {
		AvailableCandidate candidate = candidate(1L, "FOOD", "KOREAN", 37.0, 127.0, Instant.now());
		AvailablePool pool = new AvailablePool(List.of(candidate));
		// Two identical slots — only one candidate available
		List<CategorySlotCommand> slots = List.of(
				new CategorySlotCommand("FOOD", "KOREAN"),
				new CategorySlotCommand("FOOD", "KOREAN")
		);

		CourseSelectionResult result = selector.select(CourseMode.GENERAL, slots, pool, new HashMap<>(), Instant.now());

		assertThat(result.pickedPlaces()).hasSize(1);
		assertThat(result.skippedSlotIndices()).containsExactly(1);
	}

	@Test
	void popularCandidateWithoutLinkExcluded() {
		AvailableCandidate withLink = candidate(1L, "FOOD", "KOREAN", 37.0, 127.0, Instant.now());
		AvailableCandidate noLink = candidateNoLink(2L, "FOOD", "KOREAN");

		AvailablePool pool = new AvailablePool(List.of(noLink, withLink));
		List<CategorySlotCommand> slots = List.of(new CategorySlotCommand("FOOD", "KOREAN"));

		CourseSelectionResult result = selector.select(CourseMode.POPULAR, slots, pool, new HashMap<>(), Instant.now());

		assertThat(result.pickedPlaces()).hasSize(1);
		assertThat(result.pickedPlaces().get(0).getId()).isEqualTo(1L);
	}

	@Test
	void penaltyLowersReuseProbability() {
		// 동일 좌표/생성시각의 두 후보 A(id=1), B(id=2). 기본 점수는 같다.
		// A에만 cross-course 사용 패널티(2회)를 주면 B가 훨씬 자주 선택되어야 한다.
		List<CategorySlotCommand> slots = List.of(new CategorySlotCommand("FOOD", "KOREAN"));
		int iterations = 2000;
		int bChosen = 0;
		for (int i = 0; i < iterations; i++) {
			AvailableCandidate a = candidate(1L, "FOOD", "KOREAN", 37.0, 127.0, Instant.now());
			AvailableCandidate b = candidate(2L, "FOOD", "KOREAN", 37.0, 127.0, Instant.now());
			AvailablePool pool = new AvailablePool(List.of(a, b));
			Map<Long, Integer> usageCounts = new HashMap<>();
			usageCounts.put(1L, 2); // A는 이미 두 번 사용됨 → 패널티 0.3

			CourseSelectionResult result = selector.select(CourseMode.GENERAL, slots, pool, usageCounts, Instant.now());
			if (result.pickedPlaces().get(0).getId() == 2L) {
				bChosen++;
			}
		}

		// 패널티 0.3 vs 1.0 → B 선택 확률 ≈ 1/(1+0.3) ≈ 77%. 넉넉히 65% 이상이면 통과.
		assertThat(bChosen).isGreaterThan((int) (iterations * 0.65));
	}

	@Test
	void higherScoreSelectedMoreOften() {
		// prev(첫 슬롯)에 가까운 후보일수록 거리 점수가 높다.
		// 두 번째 슬롯에서 prev에 가까운 near(id=2)가 far(id=3)보다 더 자주 선택되어야 한다.
		List<CategorySlotCommand> slots = List.of(
				new CategorySlotCommand("FOOD", "KOREAN"),
				new CategorySlotCommand("CAFE", "BAKERY")
		);
		int iterations = 2000;
		int nearChosen = 0;
		for (int i = 0; i < iterations; i++) {
			AvailableCandidate anchor = candidate(1L, "FOOD", "KOREAN", 37.5000, 127.0, Instant.now());
			AvailableCandidate near = candidate(2L, "CAFE", "BAKERY", 37.5010, 127.0, Instant.now());
			AvailableCandidate far = candidate(3L, "CAFE", "BAKERY", 37.6000, 127.0, Instant.now());
			AvailablePool pool = new AvailablePool(List.of(anchor, near, far));

			CourseSelectionResult result = selector.select(CourseMode.GENERAL, slots, pool, new HashMap<>(), Instant.now());
			assertThat(result.pickedPlaces()).hasSize(2);
			if (result.pickedPlaces().get(1).getId() == 2L) {
				nearChosen++;
			}
		}

		assertThat(nearChosen).isGreaterThan(iterations / 2);
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
		return new AvailableCandidate(
				roomPlace, catCode, tagCode,
				BigDecimal.valueOf(lat), BigDecimal.valueOf(lng),
				createdAt, null, null, true, null
		);
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
		return new AvailableCandidate(
				roomPlace, catCode, tagCode, null, null,
				Instant.now(), null, null, false, null
		);
	}
}
