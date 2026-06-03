package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.application.dto.DateCoursePlaceResult;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;

final class DateCoursePlaceMapper {

	private DateCoursePlaceMapper() {
	}

	static DateCoursePlaceResult toPlaceResult(RoomPlace roomPlace, int sequenceOrder) {
		if (roomPlace == null) {
			throw new BusinessException(ErrorCode.E500_INTERNAL, "데이트 코스 장소의 RoomPlace 참조가 없습니다.");
		}
		Long roomPlaceId = roomPlace.getId();
		if (roomPlaceId == null) {
			throw new BusinessException(ErrorCode.E500_INTERNAL, "데이트 코스 장소의 roomPlaceId가 없습니다.");
		}
		Place place = roomPlace.getPlace();
		if (place == null) {
			throw new BusinessException(ErrorCode.E500_INTERNAL, "데이트 코스 장소의 Place 참조가 없습니다.");
		}
		return new DateCoursePlaceResult(
				roomPlaceId,
				place.getId(),
				place.getKakaoPlaceId(),
				place.getName(),
				place.getAddress(),
				place.getRoadAddress(),
				place.getLatitude(),
				place.getLongitude(),
				place.getServiceCategory().getCode(),
				place.getServiceCategory().getName(),
				place.getServiceTag().getCode(),
				place.getServiceTag().getName(),
				sequenceOrder
		);
	}
}
