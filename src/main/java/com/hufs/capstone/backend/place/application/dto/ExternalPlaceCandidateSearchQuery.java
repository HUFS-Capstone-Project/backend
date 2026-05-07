package com.hufs.capstone.backend.place.application.dto;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;

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
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "keyword is required.");
		}
		int normalizedLimit = limit == null ? DEFAULT_LIMIT : limit;
		if (normalizedLimit < 1 || normalizedLimit > MAX_LIMIT) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "limit must be between 1 and 15.");
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
