package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.application.dto.AvailableCandidate;
import com.hufs.capstone.backend.course.application.dto.NormalizationContext;
import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import com.hufs.capstone.backend.link.domain.LinkSourceType;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
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
		return candidate.roomPlace().getOriginRoomLink() != null;
	}

	@Override
	public NormalizationContext normalizationContext(AvailablePool pool) {
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
}
