package com.hufs.capstone.backend.place.application.dto;

import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import java.util.List;

public record ExternalPlaceCandidateSearchResult(
		List<PlaceSnapshot> items,
		int page,
		int limit,
		boolean hasNext,
		Integer nextPage,
		int totalCount,
		int pageableCount
) {
}
