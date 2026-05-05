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
		String sourceKeyword,
		String sourceSentence,
		String rawCandidate
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
			String sourceKeyword,
			String sourceSentence,
			String rawCandidate
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
				sourceKeyword,
				sourceSentence,
				rawCandidate
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
