package com.hufs.capstone.backend.place.application.dto;

import java.util.List;

public record PlaceCandidatePageResult(
		List<PlaceCandidateResult> items,
		int page,
		int limit,
		boolean hasNext,
		Integer nextPage,
		int totalCount,
		int pageableCount
) {
}
