package com.hufs.capstone.backend.place.domain;

public final class KakaoCategoryGroupPolicy {

	public static final String KAKAO_FOOD = "FD6";
	public static final String KAKAO_CAFE = "CE7";

	public static final String SERVICE_CATEGORY_FOOD = "FOOD";
	public static final String SERVICE_CATEGORY_CAFE = "CAFE";
	public static final String SERVICE_CATEGORY_ACTIVITY = "ACTIVITY";
	public static final String FALLBACK_TAG_CODE = "MISC";

	private KakaoCategoryGroupPolicy() {
	}

	public static String resolveServiceCategoryCode(String kakaoCategoryGroupCode) {
		String normalized = normalize(kakaoCategoryGroupCode);
		if (KAKAO_FOOD.equals(normalized)) {
			return SERVICE_CATEGORY_FOOD;
		}
		if (KAKAO_CAFE.equals(normalized)) {
			return SERVICE_CATEGORY_CAFE;
		}
		return SERVICE_CATEGORY_ACTIVITY;
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed.toUpperCase();
	}
}
