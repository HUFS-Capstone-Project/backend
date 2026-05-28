package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.application.dto.AvailableCandidate;
import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.course.application.dto.CourseSelectionResult;
import com.hufs.capstone.backend.course.application.dto.NormalizationContext;
import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import com.hufs.capstone.backend.link.domain.LinkSourceType;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CourseSelector {

	private final CourseScorer scorer;

	CourseSelectionResult select(
			CourseMode mode,
			List<CategorySlotCommand> slots,
			AvailablePool pool,
			Set<Long> globallyUsedIds,
			Instant plannedDateTime
	) {
		NormalizationContext ctx = buildNormalizationContext(pool, mode);
		List<Long> picked = new ArrayList<>();
		List<Integer> skipped = new ArrayList<>();
		AvailableCandidate prev = null;

		for (int i = 0; i < slots.size(); i++) {
			CategorySlotCommand slot = slots.get(i);
			final AvailableCandidate prevForLambda = prev;
			Optional<AvailableCandidate> best = pool.forSlot(slot).stream()
					.filter(c -> !globallyUsedIds.contains(c.roomPlace().getId()))
					.filter(c -> !picked.contains(c.roomPlace().getId()))
					.filter(c -> mode != CourseMode.POPULAR || c.roomPlace().getOriginRoomLink() != null)
					.max(Comparator.comparingDouble(
							c -> scorer.score(c, prevForLambda, mode, ctx, plannedDateTime)
					));

			if (best.isEmpty()) {
				skipped.add(i);
				continue;
			}

			AvailableCandidate chosen = best.get();
			picked.add(chosen.roomPlace().getId());
			prev = chosen;
		}

		List<RoomPlace> pickedPlaces = pool.all().stream()
				.filter(c -> picked.contains(c.roomPlace().getId()))
				.sorted(Comparator.comparingInt(c -> picked.indexOf(c.roomPlace().getId())))
				.map(AvailableCandidate::roomPlace)
				.toList();

		globallyUsedIds.addAll(picked);
		return new CourseSelectionResult(pickedPlaces, skipped);
	}

	private static NormalizationContext buildNormalizationContext(AvailablePool pool, CourseMode mode) {
		if (mode != CourseMode.POPULAR) {
			return new NormalizationContext(Map.of());
		}
		Map<LinkSourceType, Long> maxBySourceType = new EnumMap<>(LinkSourceType.class);
		for (AvailableCandidate candidate : pool.all()) {
			RoomLink originRoomLink = candidate.roomPlace().getOriginRoomLink();
			if (originRoomLink == null || originRoomLink.getLink() == null) {
				continue;
			}
			Link link = originRoomLink.getLink();
			if (link.getLikeCount() == null) {
				continue;
			}
			maxBySourceType.merge(link.getLinkSourceType(), link.getLikeCount(), Math::max);
		}
		return new NormalizationContext(maxBySourceType);
	}

	Set<Long> newGloballyUsedIds() {
		return new HashSet<>();
	}
}
