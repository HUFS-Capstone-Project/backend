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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * 발표용 다양성 정량 지표 측정 테스트.
 *
 * <p>장소가 "부족한" 풀(FOOD 2 / CAFE 1 / ACTIVITY 1)에서 GENERAL/TRENDY/POPULAR 3코스 배치를
 * 여러 번 생성하며 다음 지표를 산출한다.
 * <ul>
 *   <li>생성 성공률: 필요한 모든 슬롯이 채워진(skip 없는) 코스 비율</li>
 *   <li>코스 간 중복률: 코스쌍 사이에 공유되는 장소 비율</li>
 *   <li>평균 고유 장소 수: 3코스 전체에서 서로 다른 장소 수</li>
 * </ul>
 * 개선된 소프트 룰에서는 장소가 부족해도 생성 성공률이 100%여야 한다(코스 간 중복 허용 덕분).
 */
@Slf4j
class CourseDiversityMetricsTest {

	private static final long SEED = 7L;
	private static final int BATCHES = 200;
	private static final List<CourseMode> MODES =
			List.of(CourseMode.GENERAL, CourseMode.TRENDY, CourseMode.POPULAR);
	private static final List<CategorySlotCommand> SLOTS = List.of(
			new CategorySlotCommand("FOOD", null),
			new CategorySlotCommand("CAFE", null),
			new CategorySlotCommand("ACTIVITY", null)
	);

	private final CourseScorer scorer = new CourseScorer();
	private final CourseSelector selector = new CourseSelector(scorer, new Random(SEED));

	@Test
	void scarcePoolStillGeneratesAllCoursesWithDiversity() {
		int totalCourses = 0;
		int fullCourses = 0;
		double overlapSum = 0.0;
		int overlapPairs = 0;
		long uniqueSum = 0;

		for (int batch = 0; batch < BATCHES; batch++) {
			AvailablePool pool = scarcePool();
			Map<Long, Integer> usageCounts = new HashMap<>();
			List<List<Long>> courses = new ArrayList<>();

			for (CourseMode mode : MODES) {
				CourseSelectionResult selection =
						selector.select(mode, SLOTS, pool, usageCounts, Instant.now());
				List<Long> ids = selection.pickedPlaces().stream().map(RoomPlace::getId).toList();
				courses.add(ids);
				for (Long id : ids) {
					usageCounts.merge(id, 1, Integer::sum);
				}

				totalCourses++;
				if (selection.skippedSlotIndices().isEmpty()) {
					fullCourses++;
				}
			}

			// 코스쌍 간 중복률
			for (int i = 0; i < courses.size(); i++) {
				for (int j = i + 1; j < courses.size(); j++) {
					List<Long> a = courses.get(i);
					List<Long> b = courses.get(j);
					if (a.isEmpty() || b.isEmpty()) {
						continue;
					}
					long shared = a.stream().filter(b::contains).count();
					overlapSum += (double) shared / Math.max(a.size(), b.size());
					overlapPairs++;
				}
			}

			// 배치 전체 고유 장소 수
			Set<Long> unique = new HashSet<>();
			courses.forEach(unique::addAll);
			uniqueSum += unique.size();
		}

		double successRate = (double) fullCourses / totalCourses;
		double overlapRate = overlapPairs == 0 ? 0.0 : overlapSum / overlapPairs;
		double avgUnique = (double) uniqueSum / BATCHES;

		log.info("=== 개선 방식(중복 패널티 + 확률 선택) 다양성 지표 (부족 풀: FOOD2/CAFE1/ACT1) ===");
		log.info("배치 수            : {}", BATCHES);
		log.info("생성 성공률        : {}%", String.format("%.1f", successRate * 100));
		log.info("코스 간 중복률      : {}%", String.format("%.1f", overlapRate * 100));
		log.info("평균 고유 장소 수   : {}", String.format("%.2f", avgUnique));

		// 핵심 개선: 장소가 부족해도 모든 코스가 채워진다(하드 제외였다면 뒤 코스가 잘렸을 것).
		assertThat(successRate).isEqualTo(1.0);
	}

	private static AvailablePool scarcePool() {
		List<AvailableCandidate> candidates = new ArrayList<>();
		candidates.add(candidate(1L, "FOOD", "KOREAN", 37.5000, 127.0));
		candidates.add(candidate(2L, "FOOD", "CHINESE", 37.5020, 127.0));
		candidates.add(candidate(3L, "CAFE", "BAKERY", 37.5010, 127.0));
		candidates.add(candidate(4L, "ACTIVITY", "PARK", 37.5015, 127.0));
		return new AvailablePool(candidates);
	}

	private static AvailableCandidate candidate(Long id, String catCode, String tagCode, double lat, double lng) {
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
		when(roomPlace.getCreatedAt()).thenReturn(Instant.now());
		when(roomPlace.getOriginRoomLink()).thenReturn(mock(com.hufs.capstone.backend.link.domain.entity.RoomLink.class));
		return new AvailableCandidate(
				roomPlace, catCode, tagCode,
				BigDecimal.valueOf(lat), BigDecimal.valueOf(lng),
				Instant.now(), null, null, true, null
		);
	}
}
