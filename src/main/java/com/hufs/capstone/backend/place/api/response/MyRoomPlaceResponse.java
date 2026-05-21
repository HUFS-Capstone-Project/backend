package com.hufs.capstone.backend.place.api.response;

import com.hufs.capstone.backend.place.application.dto.BusinessHoursDisplayResult;
import com.hufs.capstone.backend.place.application.dto.MyRoomPlaceResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceResult;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceSourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MyRoomPlaceResponse(
		Long roomPlaceId,
		Long placeId,
		String kakaoPlaceId,
		String name,
		String address,
		String roadAddress,
		BigDecimal latitude,
		BigDecimal longitude,
		String categoryName,
		String categoryGroupCode,
		String serviceCategoryCode,
		String serviceCategoryName,
		String serviceTagCode,
		String serviceTagName,
		String sidoCode,
		String sidoName,
		String sigunguCode,
		String sigunguName,
		String memo,
		List<RoomPlaceResponse.RoomPlaceMemoResponse> memos,
		RoomPlaceSourceType sourceType,
		Long sourceRoomLinkId,
		String originalUrl,
		Long createdBy,
		Instant createdAt,
		String businessHoursStatus,
		BusinessHoursDisplayResult businessHours,
		Instant businessHoursFetchedAt,
		Instant businessHoursExpiresAt,
		RoomResponse room
) {

	public static MyRoomPlaceResponse from(MyRoomPlaceResult result) {
		RoomPlaceResult place = result.place();
		return new MyRoomPlaceResponse(
				place.roomPlaceId(),
				place.placeId(),
				place.kakaoPlaceId(),
				place.name(),
				place.address(),
				place.roadAddress(),
				place.latitude(),
				place.longitude(),
				place.categoryName(),
				place.categoryGroupCode(),
				place.serviceCategoryCode(),
				place.serviceCategoryName(),
				place.serviceTagCode(),
				place.serviceTagName(),
				place.sidoCode(),
				place.sidoName(),
				place.sigunguCode(),
				place.sigunguName(),
				place.memo(),
				place.memos().stream()
						.map(RoomPlaceResponse.RoomPlaceMemoResponse::from)
						.toList(),
				place.sourceType(),
				place.sourceRoomLinkId(),
				place.originalUrl(),
				place.createdBy(),
				place.createdAt(),
				place.businessHoursStatus(),
				place.businessHours(),
				place.businessHoursFetchedAt(),
				place.businessHoursExpiresAt(),
				RoomResponse.from(result.room())
		);
	}

	public record RoomResponse(
			String roomId,
			String roomName
	) {

		public static RoomResponse from(MyRoomPlaceResult.RoomResult result) {
			return new RoomResponse(result.roomId(), result.roomName());
		}
	}
}
