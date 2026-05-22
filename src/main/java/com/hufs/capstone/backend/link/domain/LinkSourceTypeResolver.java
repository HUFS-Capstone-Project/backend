package com.hufs.capstone.backend.link.domain;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

public final class LinkSourceTypeResolver {

	public static final String INSTAGRAM_CANONICAL_HOST = "www.instagram.com";
	public static final String NAVER_BLOG_CANONICAL_HOST = "blog.naver.com";

	private static final Set<String> INSTAGRAM_EXACT_HOSTS = Set.of("instagr.am", "instagram.com");
	private static final Set<String> NAVER_BLOG_EXACT_HOSTS = Set.of("naver.me", NAVER_BLOG_CANONICAL_HOST, "m.blog.naver.com");
	private static final Set<String> NAVER_BLOG_CANONICALIZABLE_HOSTS = Set.of(NAVER_BLOG_CANONICAL_HOST, "m.blog.naver.com");
	private static final Set<String> YOUTUBE_EXACT_HOSTS = Set.of("youtu.be", "youtube.com", "youtube-nocookie.com");

	private LinkSourceTypeResolver() {
	}

	public static LinkSourceType initialFromUrl(String url) {
		LinkSourceType resolved = fromUrl(url);
		return resolved == null ? LinkSourceType.GENERIC_WEB : resolved;
	}

	public static LinkSourceType fromUrl(String url) {
		String host = host(url);
		if (host == null) {
			return null;
		}
		return fromHost(host);
	}

	public static LinkSourceType fromProcessingSourceType(String sourceType) {
		if (sourceType == null || sourceType.isBlank()) {
			return null;
		}
		try {
			return LinkSourceType.valueOf(sourceType.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	public static LinkSourceType resolveProcessingResult(LinkSourceType current, LinkSourceType processingResult) {
		if (processingResult != null) {
			return processingResult;
		}
		return current == null ? LinkSourceType.GENERIC_WEB : current;
	}

	public static boolean isInstagramUrl(String url) {
		String host = host(url);
		return host != null && isInstagramHost(host);
	}

	public static boolean isInstagramHost(String host) {
		String normalizedHost = normalizeHost(host);
		return INSTAGRAM_EXACT_HOSTS.contains(normalizedHost)
				|| (normalizedHost != null && normalizedHost.endsWith(".instagram.com"));
	}

	public static boolean isNaverBlogHost(String host) {
		String normalizedHost = normalizeHost(host);
		return NAVER_BLOG_EXACT_HOSTS.contains(normalizedHost);
	}

	public static boolean isCanonicalizableNaverBlogHost(String host) {
		return NAVER_BLOG_CANONICALIZABLE_HOSTS.contains(normalizeHost(host));
	}

	public static boolean isYoutubeHost(String host) {
		String normalizedHost = normalizeHost(host);
		return YOUTUBE_EXACT_HOSTS.contains(normalizedHost)
				|| (normalizedHost != null && normalizedHost.endsWith(".youtube.com"))
				|| (normalizedHost != null && normalizedHost.endsWith(".youtube-nocookie.com"));
	}

	private static LinkSourceType fromHost(String host) {
		if (isInstagramHost(host)) {
			return LinkSourceType.INSTAGRAM;
		}
		if (isNaverBlogHost(host)) {
			return LinkSourceType.NAVER_BLOG;
		}
		if (isYoutubeHost(host)) {
			return LinkSourceType.YOUTUBE;
		}
		return LinkSourceType.GENERIC_WEB;
	}

	private static String host(String url) {
		if (url == null || url.isBlank()) {
			return null;
		}
		try {
			String host = URI.create(url.trim()).getHost();
			return normalizeHost(host);
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private static String normalizeHost(String host) {
		return host == null ? null : host.toLowerCase(Locale.ROOT);
	}
}
