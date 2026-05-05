package com.hufs.capstone.backend.place.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.place.application.dto.RoomPlacePageResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceSaveResult;
import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceSourceType;
import com.hufs.capstone.backend.place.domain.repository.PlaceRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceSourceRepository;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import com.hufs.capstone.backend.room.domain.repository.RoomMemberRepository;
import com.hufs.capstone.backend.room.domain.repository.RoomRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
class RoomPlaceCommandServiceIntegrationTest {

	private static final Long USER_ID = 100L;
	private static final String ROOM_PUBLIC_ID = "33333333-3333-3333-3333-333333333333";

	@Autowired
	private RoomPlaceQueryService roomPlaceQueryService;

	@Autowired
	private RoomPlaceStorageService roomPlaceStorageService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private RoomPlaceRepository roomPlaceRepository;

	@Autowired
	private RoomPlaceSourceRepository roomPlaceSourceRepository;

	@Autowired
	private PlaceRepository placeRepository;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private RoomMemberRepository roomMemberRepository;

	@Autowired
	private LinkRepository linkRepository;

	@Autowired
	private RoomLinkRepository roomLinkRepository;

	private Room room;

	@BeforeEach
	void setUp() {
		roomPlaceSourceRepository.deleteAll();
		roomPlaceRepository.deleteAll();
		placeRepository.deleteAll();
		roomLinkRepository.deleteAll();
		linkRepository.deleteAll();
		roomMemberRepository.deleteAll();
		roomRepository.deleteAll();
		room = roomRepository.saveAndFlush(Room.create(ROOM_PUBLIC_ID, "Place Room", "INVITE333333", USER_ID));
		roomMemberRepository.saveAndFlush(RoomMember.join(room, USER_ID));
	}

	@AfterEach
	void tearDown() {
		roomPlaceSourceRepository.deleteAll();
		roomPlaceRepository.deleteAll();
		placeRepository.deleteAll();
		roomLinkRepository.deleteAll();
		linkRepository.deleteAll();
		roomMemberRepository.deleteAll();
		roomRepository.deleteAll();
	}

	@Test
	void shouldAssignTaxonomyAndExternalSourceWhenSavingExternalPlace() {
		RoomPlaceSaveResult result = saveExternalForTest(foodSnapshot("123456789", "Donkatsu Place"));

		Place place = placeRepository.findWithTaxonomyByKakaoPlaceId("123456789").orElseThrow();
		RoomPlace roomPlace = roomPlaceRepository.findByIdAndRoomId(result.places().get(0).roomPlaceId(), room.getId())
				.orElseThrow();

		assertThat(place.getServiceCategory().getCode()).isEqualTo("FOOD");
		assertThat(place.getServiceTag().getCode()).isEqualTo("JAPANESE");
		assertThat(roomPlace.getSourceType()).isEqualTo(RoomPlaceSourceType.EXTERNAL_SEARCH);
		assertThat(roomPlace.getSourceRoomLinkId()).isNull();
		assertThat(roomPlace.getMemo()).isNull();
		assertThat(result.places().get(0).created()).isTrue();
		assertThat(result.places().get(0).alreadyInRoom()).isFalse();
	}

	@Test
	void shouldNotOverwriteExistingPlaceWithBlankSnapshotOrFallbackTaxonomy() {
		saveExternalForTest(foodSnapshot("123456789", "Donkatsu Place"));

		RoomPlaceSaveResult repeated = saveExternalForTest(incompleteSnapshot("123456789"));

		Place place = placeRepository.findWithTaxonomyByKakaoPlaceId("123456789").orElseThrow();

		assertThat(place.getName()).isEqualTo("Donkatsu Place");
		assertThat(place.getLongitude()).isEqualByComparingTo("126.972000000000");
		assertThat(place.getLatitude()).isEqualByComparingTo("37.570000000000");
		assertThat(place.getServiceCategory().getCode()).isEqualTo("FOOD");
		assertThat(place.getServiceTag().getCode()).isEqualTo("JAPANESE");
		assertThat(repeated.places().get(0).created()).isFalse();
		assertThat(repeated.places().get(0).alreadyInRoom()).isTrue();
	}

	@Test
	void shouldFallbackNewPlaceWithoutTaxonomySignalToActivityMisc() {
		saveExternalForTest(noTaxonomySnapshot("987654321", "No Taxonomy Place"));

		Place place = placeRepository.findWithTaxonomyByKakaoPlaceId("987654321").orElseThrow();

		assertThat(place.getServiceCategory().getCode()).isEqualTo("ACTIVITY");
		assertThat(place.getServiceTag().getCode()).isEqualTo("MISC");
	}

	@Test
	void shouldRejectSavingSnapshotWithoutKakaoPlaceId() {
		assertThatThrownBy(() -> saveExternalForTest(noTaxonomySnapshot(null, "No Kakao ID")))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E400_ILLEGAL_ARGUMENT));
	}

	@Test
	void shouldListWithoutKeywordAndFindByHangulInitialConsonants() {
		String placeName = "\uB204\uD06C\uB179";
		saveExternalForTest(noTaxonomySnapshot("555555555", placeName));

		RoomPlacePageResult allPlaces = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				null,
				null,
				null,
				0,
				20
		);
		RoomPlacePageResult initialOne = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				"\u3134",
				null,
				null,
				0,
				20
		);
		RoomPlacePageResult initialTwo = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				"\u3134\u314B",
				null,
				null,
				0,
				20
		);
		RoomPlacePageResult initialThree = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				"\u3134\u314B\u3134",
				null,
				null,
				0,
				20
		);

		assertThat(allPlaces.items()).extracting("name").contains(placeName);
		assertThat(initialOne.items()).extracting("name").contains(placeName);
		assertThat(initialTwo.items()).extracting("name").contains(placeName);
		assertThat(initialThree.items()).extracting("name").contains(placeName);
	}

	@Test
	void shouldPreserveRoomPlaceAndClearSourceRoomLinkWhenRoomLinkIsDeleted() {
		Link link = linkRepository.saveAndFlush(Link.register("https://example.com/a", "https://example.com/a", "job-a"));
		RoomLink roomLink = roomLinkRepository.saveAndFlush(RoomLink.bind(room, link));
		RoomPlaceSaveResult saved = new RoomPlaceSaveResult(
				link.getId(),
				roomPlaceStorageService.saveAll(
						room,
						USER_ID,
						List.of(foodSnapshot("123456789", "Linked Place")),
						null,
						RoomPlaceSourceType.LINK_ANALYSIS,
						roomLink
				)
		);

		roomPlaceSourceRepository.deleteByRoomPlaceId(saved.places().get(0).roomPlaceId());
		roomPlaceRepository.clearSourceRoomLinkBySourceRoomLinkId(roomLink.getId());
		roomLinkRepository.delete(roomLink);
		roomLinkRepository.flush();

		RoomPlace reloaded = roomPlaceRepository.findByIdAndRoomId(saved.places().get(0).roomPlaceId(), room.getId())
				.orElseThrow();

		assertThat(reloaded.getSourceRoomLinkId()).isNull();
		assertThat(reloaded.getPlaceName()).isEqualTo("Linked Place");
	}

	private RoomPlaceSaveResult saveExternalForTest(PlaceSnapshot snapshot) {
		return transactionTemplate.execute(status -> new RoomPlaceSaveResult(
				null,
				roomPlaceStorageService.saveAll(
						roomRepository.findById(room.getId()).orElseThrow(),
						USER_ID,
						List.of(snapshot),
						null,
						RoomPlaceSourceType.EXTERNAL_SEARCH,
						null
				)
		));
	}

	private static PlaceSnapshot foodSnapshot(String kakaoPlaceId, String name) {
		return PlaceSnapshot.kakao(
				kakaoPlaceId,
				name,
				"\uC74C\uC2DD\uC810 > \uC77C\uC2DD > \uB3C8\uAE4C\uC2A4",
				"FD6",
				"\uC74C\uC2DD\uC810",
				"02-000-0000",
				"\uC11C\uC6B8\uC2DC \uC885\uB85C\uAD6C",
				"\uC885\uB85C 1",
				new BigDecimal("126.972000000000"),
				new BigDecimal("37.570000000000"),
				"https://place.map.kakao.com/" + kakaoPlaceId,
				null,
				null,
				null,
				null
		);
	}

	private static PlaceSnapshot incompleteSnapshot(String kakaoPlaceId) {
		return PlaceSnapshot.kakao(
				kakaoPlaceId,
				" ",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);
	}

	private static PlaceSnapshot noTaxonomySnapshot(String kakaoPlaceId, String name) {
		return PlaceSnapshot.kakao(
				kakaoPlaceId,
				name,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);
	}
}
