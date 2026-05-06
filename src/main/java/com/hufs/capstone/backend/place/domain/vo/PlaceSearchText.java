package com.hufs.capstone.backend.place.domain.vo;

import java.text.Normalizer;
import java.util.Locale;

public final class PlaceSearchText {

	private static final char HANGUL_BASE = '가';
	private static final char HANGUL_END = '힣';
	private static final int HANGUL_INITIAL_UNIT = 21 * 28;
	private static final char[] HANGUL_INITIALS = {
		'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ',
		'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
		'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ',
		'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
	};

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

	public static String buildInitialConsonants(String... values) {
		StringBuilder builder = new StringBuilder();
		if (values != null) {
			for (String value : values) {
				appendInitialConsonants(builder, normalize(value));
			}
		}
		return toNullIfBlank(builder.toString());
	}

	public static String normalizeKeyword(String keyword) {
		return toNullIfBlank(normalize(keyword));
	}

	public static String initialKeyword(String keyword) {
		String normalized = normalize(keyword);
		if (normalized == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		appendInitialConsonants(builder, normalized);
		return toNullIfBlank(builder.toString());
	}

	private static void appendNormalized(StringBuilder builder, String value) {
		String normalized = normalize(value);
		if (normalized != null) {
			builder.append(normalized);
		}
	}

	private static void appendInitialConsonants(StringBuilder builder, String normalized) {
		if (normalized == null) {
			return;
		}
		for (int i = 0; i < normalized.length(); i++) {
			char ch = normalized.charAt(i);
			if (isHangulSyllable(ch)) {
				int initialIndex = (ch - HANGUL_BASE) / HANGUL_INITIAL_UNIT;
				builder.append(HANGUL_INITIALS[initialIndex]);
			} else if (isHangulInitial(ch)) {
				builder.append(ch);
			}
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
			if (Character.isLetterOrDigit(ch) || isHangulInitial(ch)) {
				builder.append(ch);
			}
		}
		return toNullIfBlank(builder.toString());
	}

	private static boolean isHangulSyllable(char ch) {
		return ch >= HANGUL_BASE && ch <= HANGUL_END;
	}

	private static boolean isHangulInitial(char ch) {
		return ch >= 'ㄱ' && ch <= 'ㅎ';
	}

	private static String toNullIfBlank(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
