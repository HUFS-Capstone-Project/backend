package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

final class LinkUrlNormalizer {

	private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
	private static final int MAX_URL_LENGTH = 2048;

	private LinkUrlNormalizer() {
	}

	static NormalizedUrl normalize(String rawUrl) {
		if (rawUrl == null || rawUrl.isBlank()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "URL은 필수입니다.");
		}
		String candidate = rawUrl.trim();
		if (candidate.length() > MAX_URL_LENGTH) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "URL 길이가 너무 깁니다.");
		}

		URI parsed;
		try {
			parsed = URI.create(candidate);
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "URL 형식이 올바르지 않습니다.", ex);
		}

		validateScheme(parsed.getScheme());
		validateHost(parsed.getHost());

		return new NormalizedUrl(candidate, candidate);
	}

	private static void validateScheme(String scheme) {
		if (scheme == null || scheme.isBlank()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "URL 스킴은 필수입니다.");
		}
		if (!ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "http/https URL만 허용합니다.");
		}
	}

	private static void validateHost(String host) {
		if (host == null || host.isBlank()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "URL 호스트는 필수입니다.");
		}
	}

	record NormalizedUrl(String originalUrl, String normalizedUrl) {
	}
}
