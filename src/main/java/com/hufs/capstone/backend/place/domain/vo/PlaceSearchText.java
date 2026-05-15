package com.hufs.capstone.backend.place.domain.vo;

import java.text.Normalizer;
import java.util.Locale;

public final class PlaceSearchText {

	private PlaceSearchText() {
	}

	public static String buildSearchText(String... values) {
		StringBuilder builder = new StringBuilder();
		if (values != null) {
			for (String value : values) {
				appendNormalized(builder, value);
			}
		}
		return toNullIfBlank(builder.toString());
	}

	public static String normalizeKeyword(String keyword) {
		return toNullIfBlank(normalize(keyword));
	}

	private static void appendNormalized(StringBuilder builder, String value) {
		String normalized = normalize(value);
		if (normalized != null) {
			builder.append(normalized);
		}
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
		StringBuilder builder = new StringBuilder(normalized.length());
		for (int i = 0; i < normalized.length(); i++) {
			char ch = normalized.charAt(i);
			if (Character.isLetterOrDigit(ch)) {
				builder.append(ch);
			}
		}
		return toNullIfBlank(builder.toString());
	}

	private static String toNullIfBlank(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
