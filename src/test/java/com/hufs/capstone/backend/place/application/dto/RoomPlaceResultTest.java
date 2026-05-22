package com.hufs.capstone.backend.place.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.hufs.capstone.backend.link.domain.LinkSourceType;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.place.api.response.RoomPlaceResponse;
import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceAddedVia;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import com.hufs.capstone.backend.region.application.dto.ResolvedRegion;
import com.hufs.capstone.backend.room.domain.entity.Room;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("null")
class RoomPlaceResultTest {

	@Test
	void shouldExposeLinkSourceTypeFromOriginRoomLinkLink() {
		Link link = Link.register(
				"https://youtu.be/abc",
				"https://youtu.be/abc",
				"job-1"
		);
		RoomPlace roomPlace = roomPlace(RoomLink.bind(room(), link));

		RoomPlaceResponse response = RoomPlaceResponse.from(
				RoomPlaceResult.from(roomPlace, null, "https://example.com/not-used")
		);

		assertThat(response.linkSourceType()).isEqualTo(LinkSourceType.YOUTUBE);
	}

	@Test
	void shouldNotParseOriginalUrlWhenOriginRoomLinkIsMissing() {
		RoomPlace roomPlace = roomPlace(null);

		RoomPlaceResult result = RoomPlaceResult.from(
				roomPlace,
				null,
				"https://www.instagram.com/p/abc/"
		);

		assertThat(result.linkSourceType()).isNull();
	}

	private static RoomPlace roomPlace(RoomLink originRoomLink) {
		PlaceSnapshot snapshot = PlaceSnapshot.kakao(
				"123456789",
				"Linked Place",
				"Food > Cafe",
				"CE7",
				null,
				"Address",
				"Road Address",
				null,
				null,
				"https://place.map.kakao.com/123456789"
		);
		PlaceCategory category = PlaceCategory.create("CAFE", "Cafe", 1, true);
		ReflectionTestUtils.setField(category, "id", 2L);
		PlaceTag tag = PlaceTag.create(category, null, "MISC", "Misc", 1, true);
		ReflectionTestUtils.setField(tag, "id", 3L);
		Place place = Place.create(snapshot, category, tag);
		ReflectionTestUtils.setField(place, "id", 10L);
		RoomPlace roomPlace = RoomPlace.create(
				room(),
				place,
				100L,
				RoomPlaceAddedVia.LINK_ANALYSIS,
				originRoomLink,
				snapshot,
				ResolvedRegion.unresolved()
		);
		ReflectionTestUtils.setField(roomPlace, "id", 20L);
		return roomPlace;
	}

	private static Room room() {
		Room room = Room.create("room-public-id", "Room", "INVITE123", 100L);
		ReflectionTestUtils.setField(room, "id", 1L);
		return room;
	}
}
