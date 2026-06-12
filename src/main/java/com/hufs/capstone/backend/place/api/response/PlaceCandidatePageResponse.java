package com.hufs.capstone.backend.place.api.response;

import com.hufs.capstone.backend.place.application.dto.PlaceCandidatePageResult;
import java.util.List;

public record PlaceCandidatePageResponse(
		List<PlaceCandidateResponse> items,
		int page,
		int limit,
		boolean hasNext,
		Integer nextPage,
		int totalCount,
		int pageableCount
) {

	public static PlaceCandidatePageResponse from(PlaceCandidatePageResult result) {
		return new PlaceCandidatePageResponse(
				result.items().stream()
						.map(PlaceCandidateResponse::from)
						.toList(),
				result.page(),
				result.limit(),
				result.hasNext(),
				result.nextPage(),
				result.totalCount(),
				result.pageableCount()
		);
	}
}
