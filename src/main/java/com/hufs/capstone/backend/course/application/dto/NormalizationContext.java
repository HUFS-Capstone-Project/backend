package com.hufs.capstone.backend.course.application.dto;

import com.hufs.capstone.backend.link.domain.LinkSourceType;
import java.util.Map;

public record NormalizationContext(
		Map<LinkSourceType, Long> maxLikeCountBySourceType
) {

	public long maxLikeCount(LinkSourceType sourceType) {
		return maxLikeCountBySourceType.getOrDefault(sourceType, 0L);
	}
}
