package com.hufs.capstone.backend.link.api.request;

import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SaveManualRoomPlaceRequest(
		@NotBlank(message = "카카오 장소 ID는 필수입니다.")
		@Size(max = 100, message = "카카오 장소 ID는 100자를 초과할 수 없습니다.")
		String kakaoPlaceId,

		@NotBlank(message = "장소 이름은 필수입니다.")
		@Size(max = 255, message = "장소 이름은 255자를 초과할 수 없습니다.")
		String name,

		@Size(max = 500, message = "지번 주소는 500자를 초과할 수 없습니다.")
		String address,

		@Size(max = 500, message = "도로명 주소는 500자를 초과할 수 없습니다.")
		String roadAddress,

		@NotNull(message = "위도는 필수입니다.")
		@DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
		@DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
		BigDecimal latitude,

		@NotNull(message = "경도는 필수입니다.")
		@DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
		@DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
		BigDecimal longitude,

		@Size(max = 500, message = "카테고리 이름은 500자를 초과할 수 없습니다.")
		String categoryName,

		@Size(max = 50, message = "카테고리 그룹 코드는 50자를 초과할 수 없습니다.")
		String categoryGroupCode,

		@Size(max = 100, message = "전화번호는 100자를 초과할 수 없습니다.")
		String phone,

		@Size(max = 2048, message = "장소 URL은 2048자를 초과할 수 없습니다.")
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
