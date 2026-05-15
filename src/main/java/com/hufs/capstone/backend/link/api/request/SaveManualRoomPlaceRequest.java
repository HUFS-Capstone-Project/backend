package com.hufs.capstone.backend.link.api.request;

import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SaveManualRoomPlaceRequest(
		@NotBlank
		@Size(max = 100)
		String kakaoPlaceId,

		@NotBlank
		@Size(max = 255)
		String name,

		@Size(max = 500)
		String address,

		@Size(max = 500)
		String roadAddress,

		@NotNull
		@DecimalMin("-90.0")
		@DecimalMax("90.0")
		BigDecimal latitude,

		@NotNull
		@DecimalMin("-180.0")
		@DecimalMax("180.0")
		BigDecimal longitude,

		@Size(max = 500)
		String categoryName,

		@Size(max = 50)
		String categoryGroupCode,

		@Size(max = 100)
		String phone,

		@Size(max = 2048)
	String placeUrl
) {

	public PlaceSnapshot toSnapshot() {
		return PlaceSnapshot.kakao(
				kakaoPlaceId,
				name,
				categoryName,
				categoryGroupCode,
				phone,
				address,
				roadAddress,
				longitude,
				latitude,
				placeUrl
		);
	}
}
