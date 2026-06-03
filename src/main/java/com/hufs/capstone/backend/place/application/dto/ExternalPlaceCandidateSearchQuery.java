package com.hufs.capstone.backend.place.application.dto;

import com.hufs.capstone.backend.global.exception.FieldValidationException;

public record ExternalPlaceCandidateSearchQuery(
		String keyword,
		String region,
		String categoryGroupCode,
		int limit
) {

	private static final int DEFAULT_LIMIT = 10;
	private static final int MAX_LIMIT = 15;

	public static ExternalPlaceCandidateSearchQuery of(
			String keyword,
			String region,
			String categoryGroupCode,
			Integer limit
	) {
		String normalizedKeyword = trimToNull(keyword);
		if (normalizedKeyword == null) {
			throw new FieldValidationException("keyword", "검색어는 필수입니다.");
		}
		int normalizedLimit = limit == null ? DEFAULT_LIMIT : limit;
		if (normalizedLimit < 1 || normalizedLimit > MAX_LIMIT) {
			throw new FieldValidationException("limit", "limit는 1~15 사이여야 합니다.", normalizedLimit);
		}
		return new ExternalPlaceCandidateSearchQuery(
				normalizedKeyword,
				trimToNull(region),
				trimToNull(categoryGroupCode),
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
