package com.hufs.capstone.backend.place.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.place.application.dto.PlaceTaxonomyCategoryResult;
import com.hufs.capstone.backend.place.application.dto.PlaceTaxonomyResult;
import com.hufs.capstone.backend.place.application.dto.PlaceTaxonomyTagResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlacePageResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceResult;
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
	private PlaceTaxonomyQueryService placeTaxonomyQueryService;

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
		String placeName = "누크녹";
		saveExternalForTest(noTaxonomySnapshot("555555555", placeName));

		RoomPlacePageResult allPlaces = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				null,
				null,
				null,
				null,
				null,
				0,
				20,
				null
		);
		RoomPlacePageResult initialOne = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				"ㄴ",
				null,
				null,
				null,
				null,
				0,
				20,
				null
		);
		RoomPlacePageResult initialTwo = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				"ㄴㅋ",
				null,
				null,
				null,
				null,
				0,
				20,
				null
		);
		RoomPlacePageResult initialThree = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				"ㄴㅋㄴ",
				null,
				null,
				null,
				null,
				0,
				20,
				null
		);

		assertThat(allPlaces.items()).extracting("name").contains(placeName);
		assertThat(initialOne.items()).extracting("name").contains(placeName);
		assertThat(initialTwo.items()).extracting("name").contains(placeName);
		assertThat(initialThree.items()).extracting("name").contains(placeName);
	}

	@Test
	void shouldExposeAllTaxonomyTagForEveryCategory() {
		PlaceTaxonomyResult result = placeTaxonomyQueryService.getPlaceTaxonomy();

		assertAllTag(firstTagOf(result, "FOOD"));
		assertAllTag(firstTagOf(result, "CAFE"));
		assertAllTag(firstTagOf(result, "ACTIVITY"));
	}

	@Test
	void shouldExposeCafeTaxonomyDisplayNames() {
		PlaceTaxonomyResult result = placeTaxonomyQueryService.getPlaceTaxonomy();

		assertThat(tagOf(result, "CAFE", "BAKERY").name()).isEqualTo("베이커리");
		assertThat(tagOf(result, "CAFE", "MISC").name()).isEqualTo("커피·디저트");
	}

	@Test
	void shouldTreatAllTagCodeAsCategoryWideSearch() {
		saveExternalForTest(foodSnapshot("123456789", "Donkatsu Place"));
		saveExternalForTest(cafeSnapshot("222222222", "Bakery Cafe"));
		saveExternalForTest(noTaxonomySnapshot("987654321", "Activity Place"));

		RoomPlacePageResult foodPlaces = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				null,
				"FOOD",
				"ALL",
				null,
				null,
				0,
				20,
				null
		);
		RoomPlacePageResult cafePlaces = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				null,
				"CAFE",
				"ALL",
				null,
				null,
				0,
				20,
				null
		);
		RoomPlacePageResult activityPlaces = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				null,
				"ACTIVITY",
				"ALL",
				null,
				null,
				0,
				20,
				null
		);

		assertThat(foodPlaces.items()).extracting("name").containsExactly("Donkatsu Place");
		assertThat(cafePlaces.items()).extracting("name").containsExactly("Bakery Cafe");
		assertThat(activityPlaces.items()).extracting("name").containsExactly("Activity Place");
	}

	@Test
	void shouldNormalizeRegionAndFilterRoomPlacesBySidoAndSigungu() {
		saveExternalForTest(regionalSnapshot(
				"111111111",
				"Jongno Place",
				"서울특별시 종로구 세종대로 1"
		));
		saveExternalForTest(regionalSnapshot(
				"222222222",
				"Gangnam Place",
				"서울 강남구 테헤란로 1"
		));
		saveExternalForTest(regionalSnapshot(
				"333333333",
				"Haeundae Place",
				"부산광역시 해운대구 해운대해변로 1"
		));

		RoomPlacePageResult seoulPlaces = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				null,
				null,
				null,
				"11",
				null,
				0,
				20,
				null
		);
		RoomPlacePageResult jongnoPlaces = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				null,
				null,
				null,
				"11",
				"11110",
				0,
				20,
				null
		);

		assertThat(seoulPlaces.items()).extracting("name")
				.containsExactlyInAnyOrder("Jongno Place", "Gangnam Place");
		assertThat(jongnoPlaces.items()).extracting("name").containsExactly("Jongno Place");
		assertThat(jongnoPlaces.items().get(0).sidoCode()).isEqualTo("11");
		assertThat(jongnoPlaces.items().get(0).sigunguCode()).isEqualTo("11110");
		assertThatThrownBy(() -> roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				null,
				null,
				null,
				null,
				"11110",
				0,
				20,
				null
		)).isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				null,
				null,
				null,
				"11",
				"26350",
				0,
				20,
				null
		)).isInstanceOf(BusinessException.class);
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

	@Test
	void shouldExposeSourceFeedUrlWhenGettingRoomPlaceDetail() {
		String feedUrl = "https://www.instagram.com/reel/source-feed";
		Link link = linkRepository.saveAndFlush(Link.register(feedUrl, feedUrl, "job-feed"));
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

		RoomPlaceResult detail = roomPlaceQueryService.getRoomPlace(
				USER_ID,
				ROOM_PUBLIC_ID,
				saved.places().get(0).roomPlaceId()
		);

		assertThat(detail.sourceRoomLinkId()).isEqualTo(roomLink.getId());
		assertThat(detail.sourceUrl()).isEqualTo(feedUrl);
	}

	@Test
	void shouldNotAttachSourceFeedUrlWhenExistingPlaceIsSavedFromAnotherLinkContext() {
		saveExternalForTest(foodSnapshot("123456789", "Linked Place"));
		String feedUrl = "https://www.instagram.com/reel/manual-source-feed";
		Link link = linkRepository.saveAndFlush(Link.register(feedUrl, feedUrl, "job-manual-feed"));
		RoomLink roomLink = roomLinkRepository.saveAndFlush(RoomLink.bind(room, link));
		RoomPlaceSaveResult savedFromLink = transactionTemplate.execute(status -> new RoomPlaceSaveResult(
				link.getId(),
				roomPlaceStorageService.saveAll(
						roomRepository.findById(room.getId()).orElseThrow(),
						USER_ID,
						List.of(foodSnapshot("123456789", "Linked Place")),
						null,
						RoomPlaceSourceType.LINK_ANALYSIS_MANUAL_SEARCH,
						roomLinkRepository.findById(roomLink.getId()).orElseThrow()
				)
		));

		RoomPlaceResult detail = roomPlaceQueryService.getRoomPlace(
				USER_ID,
				ROOM_PUBLIC_ID,
				savedFromLink.places().get(0).roomPlaceId()
		);

		assertThat(savedFromLink.places().get(0).created()).isFalse();
		assertThat(savedFromLink.places().get(0).alreadyInRoom()).isTrue();
		assertThat(detail.sourceRoomLinkId()).isNull();
		assertThat(detail.sourceUrl()).isNull();
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
				"음식점 > 일식 > 돈까스",
				"FD6",
				"음식점",
				"02-000-0000",
				"서울시 종로구",
				"종로 1",
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

	private static PlaceSnapshot cafeSnapshot(String kakaoPlaceId, String name) {
		return PlaceSnapshot.kakao(
				kakaoPlaceId,
				name,
				"음식점 > 카페 > 베이커리",
				"CE7",
				"카페",
				"02-111-1111",
				"서울시 종로구",
				"종로 2",
				new BigDecimal("126.973000000000"),
				new BigDecimal("37.571000000000"),
				"https://place.map.kakao.com/" + kakaoPlaceId,
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

	private static PlaceSnapshot regionalSnapshot(String kakaoPlaceId, String name, String address) {
		return PlaceSnapshot.kakao(
				kakaoPlaceId,
				name,
				null,
				null,
				null,
				null,
				address,
				address,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);
	}

	private static PlaceTaxonomyTagResult firstTagOf(PlaceTaxonomyResult result, String categoryCode) {
		PlaceTaxonomyCategoryResult category = result.categories().stream()
				.filter(item -> categoryCode.equals(item.code()))
				.findFirst()
				.orElseThrow();
		return category.tagGroups().get(0).tags().get(0);
	}

	private static PlaceTaxonomyTagResult tagOf(PlaceTaxonomyResult result, String categoryCode, String tagCode) {
		PlaceTaxonomyCategoryResult category = result.categories().stream()
				.filter(item -> categoryCode.equals(item.code()))
				.findFirst()
				.orElseThrow();
		return category.tagGroups().stream()
				.flatMap(group -> group.tags().stream())
				.filter(tag -> tagCode.equals(tag.code()))
				.findFirst()
				.orElseThrow();
	}

	private static void assertAllTag(PlaceTaxonomyTagResult tag) {
		assertThat(tag.code()).isEqualTo("ALL");
		assertThat(tag.name()).isEqualTo("전체");
		assertThat(tag.sortOrder()).isZero();
	}
}
