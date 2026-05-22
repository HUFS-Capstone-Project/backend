package com.hufs.capstone.backend.place.api.response;

import com.hufs.capstone.backend.link.domain.LinkSourceType;
import com.hufs.capstone.backend.place.application.dto.BusinessHoursDisplayResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceMemoResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceResult;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceAddedVia;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RoomPlaceResponse(
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
		List<RoomPlaceMemoResponse> memos,
		RoomPlaceAddedVia addedVia,
		Long originRoomLinkId,
		String originalUrl,
		LinkSourceType linkSourceType,
		Long createdBy,
		Instant createdAt,
		String businessHoursStatus,
		BusinessHoursDisplayResult businessHours,
		Instant businessHoursFetchedAt,
		Instant businessHoursExpiresAt
) {

	public static RoomPlaceResponse from(RoomPlaceResult result) {
		return new RoomPlaceResponse(
				result.roomPlaceId(),
				result.placeId(),
				result.kakaoPlaceId(),
				result.name(),
				result.address(),
				result.roadAddress(),
				result.latitude(),
				result.longitude(),
				result.categoryName(),
				result.categoryGroupCode(),
				result.serviceCategoryCode(),
				result.serviceCategoryName(),
				result.serviceTagCode(),
				result.serviceTagName(),
				result.sidoCode(),
				result.sidoName(),
				result.sigunguCode(),
				result.sigunguName(),
				result.memo(),
				result.memos().stream()
						.map(RoomPlaceMemoResponse::from)
						.toList(),
				result.addedVia(),
				result.originRoomLinkId(),
				result.originalUrl(),
				result.linkSourceType(),
				result.createdBy(),
				result.createdAt(),
				result.businessHoursStatus(),
				result.businessHours(),
				result.businessHoursFetchedAt(),
				result.businessHoursExpiresAt()
		);
	}

	public record RoomPlaceMemoResponse(
			Long userId,
			String nickname,
			String profileImageUrl,
			String memo,
			Instant updatedAt
	) {

		public static RoomPlaceMemoResponse from(RoomPlaceMemoResult result) {
			return new RoomPlaceMemoResponse(
					result.userId(),
					result.nickname(),
					result.profileImageUrl(),
					result.memo(),
					result.updatedAt()
			);
		}
	}
}
