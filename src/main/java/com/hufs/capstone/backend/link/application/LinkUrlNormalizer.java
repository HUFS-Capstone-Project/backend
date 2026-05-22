package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.domain.LinkSourceTypeResolver;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class LinkUrlNormalizer {

	private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
	private static final int MAX_URL_LENGTH = 2048;
	private static final String YOUTUBE_CANONICAL_HOST = "www.youtube.com";

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

		return new NormalizedUrl(candidate, canonicalUrl(parsed));
	}

	static boolean isInstagramCanonicalUrl(String url) {
		if (url == null || url.isBlank()) {
			return false;
		}
		try {
			URI parsed = URI.create(url.trim());
			return LinkSourceTypeResolver.INSTAGRAM_CANONICAL_HOST.equalsIgnoreCase(parsed.getHost());
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private static String canonicalUrl(URI parsed) {
		String instagramUrl = canonicalInstagramUrl(parsed);
		if (instagramUrl != null) {
			return instagramUrl;
		}
		String naverBlogUrl = canonicalNaverBlogUrl(parsed);
		if (naverBlogUrl != null) {
			return naverBlogUrl;
		}
		String youtubeUrl = canonicalYoutubeUrl(parsed);
		if (youtubeUrl != null) {
			return youtubeUrl;
		}
		return canonicalGenericUrl(parsed);
	}

	private static String canonicalInstagramUrl(URI parsed) {
		String host = lower(parsed.getHost());
		if (!LinkSourceTypeResolver.isInstagramHost(host)) {
			return null;
		}

		List<String> parts = pathParts(parsed);
		if (parts.size() < 2) {
			return null;
		}

		String mediaPath = parts.get(0).toLowerCase(Locale.ROOT);
		String shortcode = parts.get(1).trim();
		if (shortcode.isBlank()) {
			return null;
		}
		if ("reel".equals(mediaPath) || "reels".equals(mediaPath)) {
			return "https://" + LinkSourceTypeResolver.INSTAGRAM_CANONICAL_HOST + "/reel/" + shortcode + "/";
		}
		if ("p".equals(mediaPath)) {
			return "https://" + LinkSourceTypeResolver.INSTAGRAM_CANONICAL_HOST + "/p/" + shortcode + "/";
		}
		if ("tv".equals(mediaPath)) {
			return "https://" + LinkSourceTypeResolver.INSTAGRAM_CANONICAL_HOST + "/tv/" + shortcode + "/";
		}
		return null;
	}

	private static String canonicalNaverBlogUrl(URI parsed) {
		String host = lower(parsed.getHost());
		if (!LinkSourceTypeResolver.isCanonicalizableNaverBlogHost(host)) {
			return null;
		}

		List<String> parts = pathParts(parsed);
		if (parts.size() != 2) {
			return null;
		}

		String blogId = parts.get(0).trim();
		String logNo = parts.get(1).trim();
		if (blogId.isBlank() || !logNo.chars().allMatch(Character::isDigit)) {
			return null;
		}
		return "https://" + LinkSourceTypeResolver.NAVER_BLOG_CANONICAL_HOST + "/" + blogId + "/" + logNo;
	}

	private static String canonicalYoutubeUrl(URI parsed) {
		String host = lower(parsed.getHost());
		if (!LinkSourceTypeResolver.isYoutubeHost(host)) {
			return null;
		}

		String videoId = youtubeVideoId(parsed);
		if (videoId == null || videoId.isBlank()) {
			return null;
		}
		return "https://" + YOUTUBE_CANONICAL_HOST + "/watch?v=" + encode(videoId.trim());
	}

	private static String youtubeVideoId(URI parsed) {
		String host = lower(parsed.getHost());
		List<String> parts = pathParts(parsed);
		if ("youtu.be".equals(host)) {
			return parts.isEmpty() ? null : parts.get(0);
		}
		if (parts.size() >= 2 && "shorts".equalsIgnoreCase(parts.get(0))) {
			return parts.get(1);
		}
		if (parts.size() == 1 && "watch".equalsIgnoreCase(parts.get(0))) {
			return queryValue(parsed.getRawQuery(), "v");
		}
		return null;
	}

	private static String canonicalGenericUrl(URI parsed) {
		String scheme = lower(parsed.getScheme());
		String authority = lower(parsed.getRawAuthority());
		String path = parsed.getRawPath();
		if (path == null || path.isBlank()) {
			path = "/";
		} else {
			path = stripTrailingSlashes(path);
		}
		String query = canonicalQuery(parsed.getRawQuery());
		StringBuilder canonical = new StringBuilder();
		canonical.append(scheme).append("://").append(authority).append(path);
		if (query != null && !query.isBlank()) {
			canonical.append('?').append(query);
		}
		return canonical.toString();
	}

	private static String stripTrailingSlashes(String path) {
		int end = path.length();
		while (end > 0 && path.charAt(end - 1) == '/') {
			end--;
		}
		if (end == 0) {
			return "/";
		}
		return path.substring(0, end);
	}

	private static String canonicalQuery(String rawQuery) {
		if (rawQuery == null || rawQuery.isBlank()) {
			return "";
		}
		List<QueryPair> pairs = new ArrayList<>();
		for (String part : rawQuery.split("&", -1)) {
			String[] split = part.split("=", 2);
			String key = decode(split[0]);
			String value = split.length == 2 ? decode(split[1]) : "";
			pairs.add(new QueryPair(key, value));
		}
		pairs.sort(Comparator.comparing(QueryPair::key).thenComparing(QueryPair::value));
		return pairs.stream()
				.map(pair -> encode(pair.key()) + "=" + encode(pair.value()))
				.reduce((left, right) -> left + "&" + right)
				.orElse("");
	}

	private static String queryValue(String rawQuery, String targetKey) {
		if (rawQuery == null || rawQuery.isBlank()) {
			return null;
		}
		for (String part : rawQuery.split("&", -1)) {
			String[] split = part.split("=", 2);
			String key = decode(split[0]);
			if (targetKey.equals(key)) {
				return split.length == 2 ? decode(split[1]) : "";
			}
		}
		return null;
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static List<String> pathParts(URI parsed) {
		String path = parsed.getPath();
		if (path == null || path.isBlank()) {
			return List.of();
		}
		return List.of(path.split("/")).stream()
				.filter(part -> !part.isBlank())
				.toList();
	}

	private static String lower(String value) {
		return value == null ? null : value.toLowerCase(Locale.ROOT);
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

	private record QueryPair(String key, String value) {
	}
}
