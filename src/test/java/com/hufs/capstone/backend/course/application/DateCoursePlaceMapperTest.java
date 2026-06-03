package com.hufs.capstone.backend.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.course.application.dto.DateCoursePlaceResult;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DateCoursePlaceMapperTest {

	@Test
	void mapsRoomPlaceIdAndDisplayFields() {
		RoomPlace roomPlace = roomPlace(42L, place());

		DateCoursePlaceResult result = DateCoursePlaceMapper.toPlaceResult(roomPlace, 1);

		assertThat(result.roomPlaceId()).isEqualTo(42L);
		assertThat(result.sequenceOrder()).isEqualTo(1);
		assertThat(result.name()).isEqualTo("Test Place");
		assertThat(result.address()).isEqualTo("Seoul");
		assertThat(result.categoryCode()).isEqualTo("FOOD");
		assertThat(result.tagCode()).isEqualTo("KOREAN");
	}

	@Test
	void rejectsRoomPlaceWithoutId() {
		RoomPlace roomPlace = roomPlace(null, place());

		assertThatThrownBy(() -> DateCoursePlaceMapper.toPlaceResult(roomPlace, 0))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("roomPlaceId");
	}

	private static RoomPlace roomPlace(Long id, Place place) {
		RoomPlace roomPlace = mock(RoomPlace.class);
		when(roomPlace.getId()).thenReturn(id);
		when(roomPlace.getPlace()).thenReturn(place);
		return roomPlace;
	}

	private static Place place() {
		PlaceCategory category = mock(PlaceCategory.class);
		when(category.getCode()).thenReturn("FOOD");
		when(category.getName()).thenReturn("Food");

		PlaceTag tag = mock(PlaceTag.class);
		when(tag.getCode()).thenReturn("KOREAN");
		when(tag.getName()).thenReturn("Korean");

		Place place = mock(Place.class);
		when(place.getId()).thenReturn(99L);
		when(place.getKakaoPlaceId()).thenReturn("kakao-1");
		when(place.getName()).thenReturn("Test Place");
		when(place.getAddress()).thenReturn("Seoul");
		when(place.getRoadAddress()).thenReturn("Seoul Road");
		when(place.getLatitude()).thenReturn(new BigDecimal("37.5"));
		when(place.getLongitude()).thenReturn(new BigDecimal("127.0"));
		when(place.getServiceCategory()).thenReturn(category);
		when(place.getServiceTag()).thenReturn(tag);
		return place;
	}
}
