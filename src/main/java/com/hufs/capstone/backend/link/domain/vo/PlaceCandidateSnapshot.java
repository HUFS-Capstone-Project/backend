package com.hufs.capstone.backend.link.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaceCandidateSnapshot(
		String kakaoPlaceId,
		String placeName,
		String categoryName,
		String categoryGroupCode,
		String phone,
		String addressName,
		String roadAddressName,
		BigDecimal longitude,
		BigDecimal latitude,
		String placeUrl
) {
}
