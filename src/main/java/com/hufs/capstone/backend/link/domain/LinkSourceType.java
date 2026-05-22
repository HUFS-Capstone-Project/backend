package com.hufs.capstone.backend.link.domain;

import java.net.URI;
import java.util.Locale;

public enum LinkSourceType {
	INSTAGRAM,
	NAVER_BLOG,
	YOUTUBE,
	GENERIC_WEB;

	public static LinkSourceType fromUrl(String url) {
		String host = host(url);
		if (host == null) {
			return null;
		}
		if (isInstagramHost(host)) {
			return INSTAGRAM;
		}
		if (isNaverBlogHost(host)) {
			return NAVER_BLOG;
		}
		if (isYoutubeHost(host)) {
			return YOUTUBE;
		}
		return GENERIC_WEB;
	}

	private static String host(String url) {
		if (url == null || url.isBlank()) {
			return null;
		}
		try {
			String host = URI.create(url.trim()).getHost();
			return host == null ? null : host.toLowerCase(Locale.ROOT);
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private static boolean isInstagramHost(String host) {
		return "instagr.am".equals(host)
				|| "instagram.com".equals(host)
				|| host.endsWith(".instagram.com");
	}

	private static boolean isNaverBlogHost(String host) {
		return "naver.me".equals(host)
				|| "blog.naver.com".equals(host)
				|| "m.blog.naver.com".equals(host);
	}

	private static boolean isYoutubeHost(String host) {
		return "youtu.be".equals(host)
				|| "youtube.com".equals(host)
				|| host.endsWith(".youtube.com")
				|| "youtube-nocookie.com".equals(host)
				|| host.endsWith(".youtube-nocookie.com");
	}
}
