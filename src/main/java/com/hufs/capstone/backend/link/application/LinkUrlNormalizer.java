package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

final class LinkUrlNormalizer {

	private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
	private static final int MAX_URL_LENGTH = 2048;
	private static final String INSTAGRAM_HOST = "www.instagram.com";
	private static final Set<String> INSTAGRAM_CONTENT_SEGMENTS = Set.of("p", "reel", "tv");

	private LinkUrlNormalizer() {
	}

	static NormalizedUrl normalize(String rawUrl) {
		if (rawUrl == null || rawUrl.isBlank()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "URL is required.");
		}
		String candidate = rawUrl.trim();
		if (candidate.length() > MAX_URL_LENGTH) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "URL is too long.");
		}

		URI parsed;
		try {
			parsed = URI.create(candidate);
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "Invalid URL format.", ex);
		}

		String scheme = normalizedScheme(parsed.getScheme());
		String host = normalizedHost(parsed.getHost());
		int port = normalizePort(parsed.getPort(), scheme);
		String path = normalizePath(host, parsed.getPath());

		StringBuilder normalized = new StringBuilder();
		normalized.append(scheme).append("://").append(host);
		if (port != -1) {
			normalized.append(":").append(port);
		}
		normalized.append(path);

		return new NormalizedUrl(candidate, normalized.toString());
	}

	private static String normalizedScheme(String scheme) {
		if (scheme == null || scheme.isBlank()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "URL scheme is required.");
		}
		String normalized = scheme.toLowerCase(Locale.ROOT);
		if (!ALLOWED_SCHEMES.contains(normalized)) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "Only http/https URL is allowed.");
		}
		return normalized;
	}

	private static String normalizedHost(String host) {
		if (host == null || host.isBlank()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "URL host is required.");
		}
		String normalized = host.toLowerCase(Locale.ROOT).trim();
		if (normalized.startsWith("www.")) {
			normalized = normalized.substring(4);
		}
		if (normalized.equals("instagram.com")) {
			return INSTAGRAM_HOST;
		}
		return normalized;
	}

	private static int normalizePort(int port, String scheme) {
		if (port < 0) {
			return -1;
		}
		if ("http".equals(scheme) && port == 80) {
			return -1;
		}
		if ("https".equals(scheme) && port == 443) {
			return -1;
		}
		return port;
	}

	private static String normalizePath(String host, String rawPath) {
		String path = (rawPath == null || rawPath.isBlank()) ? "/" : rawPath.trim();
		path = path.replaceAll("/{2,}", "/");
		if (path.length() > 1 && path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}

		if (INSTAGRAM_HOST.equals(host)) {
			String[] segments = path.split("/");
			if (segments.length >= 3) {
				String contentType = segments[1].toLowerCase(Locale.ROOT);
				String contentId = segments[2];
				if (INSTAGRAM_CONTENT_SEGMENTS.contains(contentType) && !contentId.isBlank()) {
					return "/" + contentType + "/" + contentId;
				}
			}
		}

		return path.isBlank() ? "/" : path;
	}

	record NormalizedUrl(String originalUrl, String normalizedUrl) {
	}
}
