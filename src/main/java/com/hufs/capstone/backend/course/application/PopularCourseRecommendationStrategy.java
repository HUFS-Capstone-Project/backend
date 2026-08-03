package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.application.dto.AvailableCandidate;
import com.hufs.capstone.backend.course.application.dto.NormalizationContext;
import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import com.hufs.capstone.backend.link.domain.LinkSourceType;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class PopularCourseRecommendationStrategy implements CourseRecommendationStrategy {

	@Override
	public CourseMode mode() {
		return CourseMode.POPULAR;
	}

	@Override
	public boolean isCandidateAllowed(AvailableCandidate candidate) {
		return candidate.hasOriginLink();
	}

	@Override
	public NormalizationContext normalizationContext(AvailablePool pool) {
		Map<LinkSourceType, Long> maxBySourceType = new EnumMap<>(LinkSourceType.class);
		for (AvailableCandidate candidate : pool.all()) {
			if (!candidate.hasOriginLink() || candidate.linkSourceType() == null) {
				continue;
			}
			if (candidate.likeCount() == null) {
				continue;
			}
			maxBySourceType.merge(candidate.linkSourceType(), candidate.likeCount(), Math::max);
		}
		return new NormalizationContext(maxBySourceType);
	}
}
