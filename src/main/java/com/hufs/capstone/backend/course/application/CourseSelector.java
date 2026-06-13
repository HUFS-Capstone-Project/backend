package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.application.dto.AvailableCandidate;
import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.course.application.dto.CourseSelectionResult;
import com.hufs.capstone.backend.course.application.dto.NormalizationContext;
import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class CourseSelector {

	/**
	 * 점수 차이를 얼마나 강하게 선택 확률에 반영할지(>1 일수록 최고점 우세). "중간 강도".
	 */
	private static final double SELECTION_SHARPNESS = 2.0;

	private final CourseScorer scorer;
	private final List<CourseRecommendationStrategy> strategies;
	private final Random random;

	@Autowired
	CourseSelector(CourseScorer scorer, List<CourseRecommendationStrategy> strategies) {
		this(scorer, strategies, new Random());
	}

	CourseSelector(CourseScorer scorer, List<CourseRecommendationStrategy> strategies, Random random) {
		this.scorer = scorer;
		this.strategies = strategies;
		this.random = random;
	}

	CourseSelector(CourseScorer scorer) {
		this(scorer, defaultStrategies(), new Random());
	}

	CourseSelector(CourseScorer scorer, Random random) {
		this(scorer, defaultStrategies(), random);
	}

	private static List<CourseRecommendationStrategy> defaultStrategies() {
		return List.of(
				new GeneralCourseRecommendationStrategy(),
				new TrendyCourseRecommendationStrategy(),
				new PopularCourseRecommendationStrategy()
		);
	}

	/**
	 * 코스 하나를 구성한다.
	 *
	 * <p>코스 <b>내부</b> 장소 중복은 금지하지만, 코스 <b>간</b> 중복은 허용한다.
	 * 다른 코스에서 이미 사용된 장소({@code usageCounts})에는 점수 패널티를 적용해
	 * 다양성을 유도하되 완전히 제외하지는 않는다(소프트 룰).
	 * 슬롯별 선택은 최고점 고정 선택이 아니라 점수 기반 가중 확률 선택이다.
	 *
	 * @param usageCounts 장소(roomPlace id)별 "이전 코스들에서 사용된 횟수". 읽기 전용으로 사용하며
	 *                    카운트 증가는 호출 측(서비스)이 코스 커밋 후 수행한다.
	 */
	CourseSelectionResult select(
			CourseMode mode,
			List<CategorySlotCommand> slots,
			AvailablePool pool,
			Map<Long, Integer> usageCounts,
			Instant startDateTime
	) {
		CourseRecommendationStrategy strategy = strategyFor(mode);
		NormalizationContext ctx = strategy.normalizationContext(pool);
		List<RoomPlace> pickedPlaces = new ArrayList<>();
		Set<Long> pickedIds = new HashSet<>();
		List<Integer> skipped = new ArrayList<>();
		AvailableCandidate prev = null;

		for (int i = 0; i < slots.size(); i++) {
			CategorySlotCommand slot = slots.get(i);
			final AvailableCandidate prevForLambda = prev;
			List<AvailableCandidate> candidates = pool.forSlot(slot).stream()
					.filter(c -> !pickedIds.contains(c.roomPlace().getId()))
					.filter(strategy::isCandidateAllowed)
					.toList();

			if (candidates.isEmpty()) {
				skipped.add(i);
				continue;
			}

			AvailableCandidate chosen = weightedPick(candidates, c -> {
				double base = scorer.score(c, prevForLambda, mode, ctx, startDateTime);
				int usage = usageCounts.getOrDefault(c.roomPlace().getId(), 0);
				return Math.pow(base, SELECTION_SHARPNESS) * crossCoursePenalty(usage);
			});

			pickedPlaces.add(chosen.roomPlace());
			pickedIds.add(chosen.roomPlace().getId());
			prev = chosen;
		}

		return new CourseSelectionResult(pickedPlaces, skipped);
	}

	/**
	 * 이미 다른 코스에서 사용된 횟수에 따른 점수 패널티 배수.
	 * 0회: 1.0, 1회: 0.6, 2회: 0.3, 3회: 0.15 … (사용될수록 절반씩 감소)
	 */
	private static double crossCoursePenalty(int usageCount) {
		if (usageCount <= 0) {
			return 1.0;
		}
		return 0.6 * Math.pow(0.5, usageCount - 1);
	}

	/**
	 * 가중치(weight)에 비례하는 확률로 후보 하나를 선택한다(Weighted Random Selection).
	 * 모든 가중치가 0 이하이면 균등 확률로 선택한다.
	 */
	private AvailableCandidate weightedPick(
			List<AvailableCandidate> candidates,
			java.util.function.ToDoubleFunction<AvailableCandidate> weightFn
	) {
		double[] weights = new double[candidates.size()];
		double total = 0.0;
		for (int i = 0; i < candidates.size(); i++) {
			double w = Math.max(0.0, weightFn.applyAsDouble(candidates.get(i)));
			weights[i] = w;
			total += w;
		}

		if (total <= 0.0) {
			return candidates.get(random.nextInt(candidates.size()));
		}

		double threshold = random.nextDouble() * total;
		double cumulative = 0.0;
		for (int i = 0; i < candidates.size(); i++) {
			cumulative += weights[i];
			if (threshold < cumulative) {
				return candidates.get(i);
			}
		}
		return candidates.get(candidates.size() - 1);
	}

	private CourseRecommendationStrategy strategyFor(CourseMode mode) {
		return strategies.stream()
				.filter(strategy -> strategy.supports(mode))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unsupported course mode: " + mode));
	}
}
