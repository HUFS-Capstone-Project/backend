package com.hufs.capstone.backend.place.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.hufs.capstone.backend.global.config.JpaAuditingConfig;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceAddedVia;
import com.hufs.capstone.backend.place.application.port.RoomPlaceSearchPort;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import com.hufs.capstone.backend.region.domain.vo.ResolvedRegion;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@Import({JpaAuditingConfig.class, QuerydslRoomPlaceSearchAdapter.class})
class QuerydslRoomPlaceSearchAdapterTest {

	private static final long USER_ID = 100L;
	private static final ResolvedRegion REGION = new ResolvedRegion("11", "서울", "11440", "마포구");

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private RoomPlaceRepository repository;

	@Autowired
	private RoomPlaceSearchPort searchPort;

	private Long roomId;
	private List<Long> roomPlaceIds;

	@BeforeEach
	void setUp() {
		PlaceCategory category = entityManager.persist(PlaceCategory.create("FOOD", "음식", 1, true));
		PlaceTag tag = entityManager.persist(PlaceTag.create(category, null, "KOREAN", "한식", 1, true));
		Room room = entityManager.persist(Room.create(
				UUID.randomUUID().toString(),
				"테스트 Room",
				UUID.randomUUID().toString().replace("-", ""),
				USER_ID
		));
		entityManager.persist(RoomMember.join(room, USER_ID));

		Link link = entityManager.persist(Link.register(
				"https://example.com/place",
				"https://example.com/place",
				"job-1"
		));
		RoomLink roomLink = entityManager.persist(RoomLink.bind(room, link));

		RoomPlace linkedRoomPlace = persistRoomPlace(room, category, tag, roomLink, "place-1");
		RoomPlace directRoomPlace = persistRoomPlace(room, category, tag, null, "place-2");
		entityManager.flush();

		roomId = room.getId();
		roomPlaceIds = List.of(linkedRoomPlace.getId(), directRoomPlace.getId());
		entityManager.clear();
	}

	@Test
	void toOneFetchJoinsReturnEachRoomPlaceOnceWithoutDistinct() {
		List<RoomPlace> roomPlaces = searchPort.searchRoomPlacesAfterCursor(
				roomId, null, null, null, null, null, null, null, null, 10
		);
		List<RoomPlace> myRoomPlaces = searchPort.searchMyRoomPlacesAfterCursor(
				USER_ID, null, null, null, null, null, null, null, 10
		);

		assertThat(roomPlaces).extracting(RoomPlace::getId).containsExactlyInAnyOrderElementsOf(roomPlaceIds);
		assertThat(myRoomPlaces).extracting(RoomPlace::getId).containsExactlyInAnyOrderElementsOf(roomPlaceIds);
		assertThat(roomPlaces).filteredOn(roomPlace -> roomPlace.getOriginRoomLinkId() == null).hasSize(1);
		assertThat(roomPlaces).filteredOn(roomPlace -> roomPlace.getOriginRoomLinkId() != null).hasSize(1);
	}

	@Test
	void shouldReturnOnlyTheLatestAccessibleRoomPlaceForTheSamePlace() {
		RoomPlace firstSaved = repository.findById(roomPlaceIds.get(0)).orElseThrow();
		Room newerRoom = entityManager.persist(Room.create(
				UUID.randomUUID().toString(),
				"Newer Room",
				UUID.randomUUID().toString().replace("-", ""),
				USER_ID
		));
		entityManager.persist(RoomMember.join(newerRoom, USER_ID));
		PlaceSnapshot snapshot = PlaceSnapshot.kakao(
				firstSaved.getKakaoPlaceId(),
				"Latest saved place",
				"Food > Korean",
				"FD6",
				null,
				"Seoul Mapo-gu",
				"Seoul Mapo-gu",
				null,
				null,
				"https://place.map.kakao.com/" + firstSaved.getKakaoPlaceId()
		);
		RoomPlace latestSaved = entityManager.persist(RoomPlace.create(
				newerRoom,
				firstSaved.getPlace(),
				USER_ID,
				RoomPlaceAddedVia.EXTERNAL_SEARCH,
				null,
				snapshot,
				REGION
		));
		entityManager.flush();
		entityManager.clear();

		List<RoomPlace> result = searchPort.searchMyRoomPlacesAfterCursor(
				USER_ID, null, null, null, null, null, null, null, 10
		);

		assertThat(result).extracting(RoomPlace::getId)
				.contains(latestSaved.getId(), roomPlaceIds.get(1))
				.doesNotContain(firstSaved.getId());
	}

	private RoomPlace persistRoomPlace(
			Room room,
			PlaceCategory category,
			PlaceTag tag,
			RoomLink originRoomLink,
			String kakaoPlaceId
	) {
		PlaceSnapshot snapshot = PlaceSnapshot.kakao(
				kakaoPlaceId, "장소 " + kakaoPlaceId, "음식점 > 한식", "FD6", null,
				"서울 마포구", "서울 마포구", null, null, "https://place.map.kakao.com/" + kakaoPlaceId
		);
		Place place = entityManager.persist(Place.create(snapshot, category, tag));
		return entityManager.persist(RoomPlace.create(
				room, place, USER_ID, RoomPlaceAddedVia.EXTERNAL_SEARCH, originRoomLink, snapshot, REGION
		));
	}
}
