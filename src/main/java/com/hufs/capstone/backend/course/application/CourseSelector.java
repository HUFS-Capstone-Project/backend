package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.application.dto.AvailableCandidate;
import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.course.application.dto.CourseSelectionResult;
import com.hufs.capstone.backend.course.application.dto.NormalizationContext;
import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class CourseSelector {

	private final CourseScorer scorer;
	private final List<CourseRecommendationStrategy> strategies;

	@Autowired
	CourseSelector(CourseScorer scorer, List<CourseRecommendationStrategy> strategies) {
		this.scorer = scorer;
		this.strategies = strategies;
	}

	CourseSelector(CourseScorer scorer) {
		this(scorer, List.of(
				new GeneralCourseRecommendationStrategy(),
				new TrendyCourseRecommendationStrategy(),
				new PopularCourseRecommendationStrategy()
		));
	}

	CourseSelectionResult select(
			CourseMode mode,
			List<CategorySlotCommand> slots,
			AvailablePool pool,
			Set<Long> globallyUsedIds,
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
			Optional<AvailableCandidate> best = pool.forSlot(slot).stream()
					.filter(c -> !globallyUsedIds.contains(c.roomPlace().getId()))
					.filter(c -> !pickedIds.contains(c.roomPlace().getId()))
					.filter(strategy::isCandidateAllowed)
					.max(Comparator.comparingDouble(
							c -> scorer.score(c, prevForLambda, mode, ctx, startDateTime)
					));

			if (best.isEmpty()) {
				skipped.add(i);
				continue;
			}

			AvailableCandidate chosen = best.get();
			pickedPlaces.add(chosen.roomPlace());
			pickedIds.add(chosen.roomPlace().getId());
			prev = chosen;
		}

		globallyUsedIds.addAll(pickedIds);
		return new CourseSelectionResult(pickedPlaces, skipped);
	}

	private CourseRecommendationStrategy strategyFor(CourseMode mode) {
		return strategies.stream()
				.filter(strategy -> strategy.supports(mode))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unsupported course mode: " + mode));
	}

	Set<Long> newGloballyUsedIds() {
		return new HashSet<>();
	}
}
