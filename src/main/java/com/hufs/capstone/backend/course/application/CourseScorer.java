package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.application.dto.AvailableCandidate;
import com.hufs.capstone.backend.course.application.dto.NormalizationContext;
import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import com.hufs.capstone.backend.link.domain.LinkSourceType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
class CourseScorer {

	private static final double DIST_WEIGHT = 0.6;
	private static final double MODE_WEIGHT = 0.4;

	double score(
			AvailableCandidate candidate,
			AvailableCandidate prev,
			CourseMode mode,
			NormalizationContext ctx,
			Instant startDateTime
	) {
		double distScore = distScore(candidate, prev);
		double modeWeight = modeWeight(candidate, mode, ctx, startDateTime);
		return DIST_WEIGHT * distScore + MODE_WEIGHT * modeWeight;
	}

	private static double distScore(AvailableCandidate candidate, AvailableCandidate prev) {
		if (prev == null) {
			return 1.0;
		}
		if (prev.latitude() == null || prev.longitude() == null
				|| candidate.latitude() == null || candidate.longitude() == null) {
			return 1.0;
		}
		double dist = Haversine.km(
				prev.latitude(), prev.longitude(),
				candidate.latitude(), candidate.longitude()
		);
		return 1.0 / (1.0 + dist);
	}

	private static double modeWeight(
			AvailableCandidate candidate,
			CourseMode mode,
			NormalizationContext ctx,
			Instant startDateTime
	) {
		return switch (mode) {
			case GENERAL -> 1.0;
			case TRENDY -> trendyWeight(candidate, startDateTime);
			case POPULAR -> popularWeight(candidate, ctx);
		};
	}

	private static double trendyWeight(AvailableCandidate candidate, Instant startDateTime) {
		Instant savedAt = candidate.createdAt();
		long daysSince = Math.max(0L, ChronoUnit.DAYS.between(savedAt, startDateTime));
		return 1.0 + 0.5 * Math.exp(-daysSince / 30.0);
	}

	private static double popularWeight(AvailableCandidate candidate, NormalizationContext ctx) {
		if (!candidate.hasOriginLink() || candidate.linkSourceType() == null) {
			return 1.0;
		}
		LinkSourceType sourceType = candidate.linkSourceType();
		long likeCount = candidate.likeCount() != null ? candidate.likeCount() : 0L;
		long maxLikeCount = ctx.maxLikeCount(sourceType);
		if (maxLikeCount <= 0) {
			return 1.0;
		}
		return 1.0 + 0.8 * ((double) likeCount / maxLikeCount);
	}
}
