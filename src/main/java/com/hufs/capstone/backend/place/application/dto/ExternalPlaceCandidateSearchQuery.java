package com.hufs.capstone.backend.place.application.dto;

import com.hufs.capstone.backend.global.exception.FieldValidationException;

public record ExternalPlaceCandidateSearchQuery(
		String keyword,
		String region,
		String categoryGroupCode,
		int page,
		int limit
) {

	private static final int DEFAULT_PAGE = 1;
	private static final int MAX_PAGE = 45;
	private static final int DEFAULT_LIMIT = 10;
	private static final int MAX_LIMIT = 15;

	public static ExternalPlaceCandidateSearchQuery of(
			String keyword,
			String region,
			String categoryGroupCode,
			Integer limit
	) {
		return of(keyword, region, categoryGroupCode, DEFAULT_PAGE, limit);
	}

	public static ExternalPlaceCandidateSearchQuery of(
			String keyword,
			String region,
			String categoryGroupCode,
			Integer page,
			Integer limit
	) {
		String normalizedKeyword = trimToNull(keyword);
		if (normalizedKeyword == null) {
			throw new FieldValidationException("keyword", "keyword is required.");
		}
		int normalizedPage = page == null ? DEFAULT_PAGE : page;
		if (normalizedPage < 1 || normalizedPage > MAX_PAGE) {
			throw new FieldValidationException("page", "page must be between 1 and 45.", normalizedPage);
		}
		int normalizedLimit = limit == null ? DEFAULT_LIMIT : limit;
		if (normalizedLimit < 1 || normalizedLimit > MAX_LIMIT) {
			throw new FieldValidationException("limit", "limit must be between 1 and 15.", normalizedLimit);
		}
		return new ExternalPlaceCandidateSearchQuery(
				normalizedKeyword,
				trimToNull(region),
				trimToNull(categoryGroupCode),
				normalizedPage,
				normalizedLimit
		);
	}

	public String kakaoSearchQuery() {
		if (region == null) {
			return keyword;
		}
		return region + " " + keyword;
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
