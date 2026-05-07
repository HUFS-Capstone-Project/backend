package com.hufs.capstone.backend.place.domain.vo;

import com.hufs.capstone.backend.place.domain.enums.PlaceSource;
import java.math.BigDecimal;

public record PlaceSnapshot(
		PlaceSource source,
		String externalPlaceId,
		String kakaoPlaceId,
		String name,
		String categoryName,
		String categoryGroupCode,
		String categoryGroupName,
		String phone,
		String address,
		String roadAddress,
		BigDecimal longitude,
		BigDecimal latitude,
		String placeUrl,
		BigDecimal confidence,
		String query,
		String evidenceText,
		String originalText
) {

	public static PlaceSnapshot kakao(
			String kakaoPlaceId,
			String name,
			String categoryName,
			String categoryGroupCode,
			String categoryGroupName,
			String phone,
			String address,
			String roadAddress,
			BigDecimal longitude,
			BigDecimal latitude,
			String placeUrl,
			BigDecimal confidence,
			String query,
			String evidenceText,
			String originalText
	) {
		return new PlaceSnapshot(
				PlaceSource.KAKAO,
				kakaoPlaceId,
				kakaoPlaceId,
				name,
				categoryName,
				categoryGroupCode,
				categoryGroupName,
				phone,
				address,
				roadAddress,
				longitude,
				latitude,
				placeUrl,
				confidence,
				query,
				evidenceText,
				originalText
		);
	}

	public boolean hasKakaoPlaceId() {
		return kakaoPlaceId != null && !kakaoPlaceId.isBlank();
	}

	public boolean hasTaxonomySignal() {
		return hasText(categoryGroupCode) || hasText(categoryName);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
