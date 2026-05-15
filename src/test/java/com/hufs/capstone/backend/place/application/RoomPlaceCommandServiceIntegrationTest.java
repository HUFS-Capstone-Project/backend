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
import com.hufs.capstone.backend.place.application.dto.MyRoomPlacePageResult;
import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceSourceType;
import com.hufs.capstone.backend.place.domain.repository.PlaceBusinessHoursRepository;
import com.hufs.capstone.backend.place.domain.repository.PlaceRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceMemoRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceSourceRepository;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import com.hufs.capstone.backend.room.domain.repository.RoomMemberRepository;
import com.hufs.capstone.backend.room.domain.repository.RoomRepository;
import com.hufs.capstone.backend.user.domain.entity.User;
import com.hufs.capstone.backend.user.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
	private RoomPlaceManagementService roomPlaceManagementService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private RoomPlaceRepository roomPlaceRepository;

	@Autowired
	private PlaceBusinessHoursRepository placeBusinessHoursRepository;

	@Autowired
	private RoomPlaceSourceRepository roomPlaceSourceRepository;

	@Autowired
	private RoomPlaceMemoRepository roomPlaceMemoRepository;

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

	@Autowired
	private UserRepository userRepository;

	private Room room;

	@BeforeEach
	void setUp() {
		placeBusinessHoursRepository.deleteAll();
		roomPlaceMemoRepository.deleteAll();
		roomPlaceSourceRepository.deleteAll();
		roomPlaceRepository.deleteAll();
		placeRepository.deleteAll();
		roomLinkRepository.deleteAll();
		linkRepository.deleteAll();
		roomMemberRepository.deleteAll();
		roomRepository.deleteAll();
		userRepository.deleteAll();
		room = roomRepository.saveAndFlush(Room.create(ROOM_PUBLIC_ID, "Place Room", "INVITE333333", USER_ID));
		roomMemberRepository.saveAndFlush(RoomMember.join(room, USER_ID));
	}

	@AfterEach
	void tearDown() {
		placeBusinessHoursRepository.deleteAll();
		roomPlaceMemoRepository.deleteAll();
		roomPlaceSourceRepository.deleteAll();
		roomPlaceRepository.deleteAll();
		placeRepository.deleteAll();
		roomLinkRepository.deleteAll();
		linkRepository.deleteAll();
		roomMemberRepository.deleteAll();
		roomRepository.deleteAll();
		userRepository.deleteAll();
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
	void shouldListWithoutKeywordAndIgnoreNonMatchingKeyword() {
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
		RoomPlacePageResult keywordOne = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				"missing",
				null,
				null,
				null,
				null,
				0,
				20,
				null
		);
		RoomPlacePageResult keywordTwo = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				"absent",
				null,
				null,
				null,
				null,
				0,
				20,
				null
		);
		RoomPlacePageResult keywordThree = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				"unknown",
				null,
				null,
				null,
				null,
				0,
				20,
				null
		);

		assertThat(allPlaces.items()).extracting("name").contains(placeName);
		assertThat(keywordOne.items()).extracting("name").doesNotContain(placeName);
		assertThat(keywordTwo.items()).extracting("name").doesNotContain(placeName);
		assertThat(keywordThree.items()).extracting("name").doesNotContain(placeName);
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
		assertThat(tagOf(result, "CAFE", "COFFEE_DESSERT").name()).isEqualTo("커피·디저트");
		assertThat(tagOf(result, "CAFE", "MISC").name()).isEqualTo("기타");
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
	void shouldFilterRoomPlacesByCreator() {
		Long friendUserId = 200L;
		roomMemberRepository.saveAndFlush(RoomMember.join(room, friendUserId));
		saveExternalForTest(USER_ID, foodSnapshot("123456789", "My Place"));
		saveExternalForTest(friendUserId, cafeSnapshot("222222222", "Friend Place"));

		RoomPlacePageResult myPlaces = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				null,
				null,
				null,
				null,
				null,
				USER_ID,
				0,
				20,
				null
		);
		RoomPlacePageResult friendPlaces = roomPlaceQueryService.searchRoomPlaces(
				USER_ID,
				ROOM_PUBLIC_ID,
				null,
				null,
				null,
				null,
				null,
				friendUserId,
				0,
				20,
				null
		);

		assertThat(myPlaces.items()).extracting("name").containsExactly("My Place");
		assertThat(friendPlaces.items()).extracting("name").containsExactly("Friend Place");
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

	@Test
	void shouldKeepSeparateRoomPlaceMemosPerMemberAndExposeProfiles() {
		User firstUser = userRepository.saveAndFlush(User.register(
				"first@example.com",
				true,
				"First",
				"https://example.com/first.png"
		));
		User secondUser = userRepository.saveAndFlush(User.register(
				"second@example.com",
				true,
				"Second",
				"https://example.com/second.png"
		));
		roomMemberRepository.saveAndFlush(RoomMember.join(room, firstUser.getId()));
		roomMemberRepository.saveAndFlush(RoomMember.join(room, secondUser.getId()));
		RoomPlaceSaveResult saved = saveExternalForTest(firstUser.getId(), foodSnapshot("123456789", "Memo Place"));
		Long roomPlaceId = saved.places().get(0).roomPlaceId();

		roomPlaceManagementService.updateMemo(firstUser.getId(), ROOM_PUBLIC_ID, roomPlaceId, "first memo");
		roomPlaceManagementService.updateMemo(secondUser.getId(), ROOM_PUBLIC_ID, roomPlaceId, "second memo");

		RoomPlaceResult detail = roomPlaceQueryService.getRoomPlace(firstUser.getId(), ROOM_PUBLIC_ID, roomPlaceId);

		assertThat(detail.memo()).isEqualTo("first memo");
		assertThat(detail.memos()).hasSize(2);
		assertThat(detail.memos()).extracting("userId")
				.containsExactly(firstUser.getId(), secondUser.getId());
		assertThat(detail.memos()).extracting("nickname")
				.containsExactly("First", "Second");
		assertThat(detail.memos()).extracting("profileImageUrl")
				.containsExactly("https://example.com/first.png", "https://example.com/second.png");
		assertThat(detail.memos()).extracting("memo")
				.containsExactly("first memo", "second memo");
	}

	@Test
	void shouldSearchMyRoomPlacesOnlyForCurrentMemberAndCreator() {
		Long friendUserId = 200L;
		roomMemberRepository.saveAndFlush(RoomMember.join(room, friendUserId));
		Room inaccessibleRoom = roomRepository.saveAndFlush(Room.create(
				"44444444-4444-4444-4444-444444444444",
				"Hidden Room",
				"INVITE444444",
				USER_ID
		));
		saveExternalForTest(USER_ID, foodSnapshot("111111111", "My Accessible Place"));
		saveExternalForTest(friendUserId, cafeSnapshot("222222222", "Friend Accessible Place"));
		saveExternalForTest(inaccessibleRoom, USER_ID, noTaxonomySnapshot("333333333", "My Hidden Place"));

		MyRoomPlacePageResult result = roomPlaceQueryService.searchMyRoomPlaces(
				USER_ID,
				null,
				null,
				null,
				null,
				null,
				0,
				20,
				null
		);

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).place().name()).isEqualTo("My Accessible Place");
		assertThat(result.items().get(0).room().roomId()).isEqualTo(ROOM_PUBLIC_ID);
		assertThat(result.items().get(0).room().roomName()).isEqualTo("Place Room");
	}

	@Test
	void shouldExposeSameKakaoPlaceInDifferentRoomsAsSeparateMyRoomPlaceItems() {
		Room secondRoom = roomRepository.saveAndFlush(Room.create(
				"55555555-5555-5555-5555-555555555555",
				"Second Room",
				"INVITE555555",
				USER_ID
		));
		roomMemberRepository.saveAndFlush(RoomMember.join(secondRoom, USER_ID));
		saveExternalForTest(USER_ID, foodSnapshot("123456789", "Shared Place"));
		saveExternalForTest(secondRoom, USER_ID, foodSnapshot("123456789", "Shared Place"));

		MyRoomPlacePageResult result = roomPlaceQueryService.searchMyRoomPlaces(
				USER_ID,
				null,
				null,
				null,
				null,
				null,
				0,
				20,
				null
		);

		assertThat(result.items()).hasSize(2);
		assertThat(result.items()).extracting(item -> item.place().kakaoPlaceId())
				.containsExactly("123456789", "123456789");
		assertThat(result.items()).extracting(item -> item.room().roomId())
				.containsExactly(secondRoom.getPublicId(), ROOM_PUBLIC_ID);
		assertThat(result.items()).extracting(item -> item.place().roomPlaceId())
				.doesNotHaveDuplicates();
	}

	@Test
	void shouldFilterAndPageMyRoomPlacesLikeRoomPlaceList() {
		saveExternalForTest(foodSnapshot("123456789", "Donkatsu Place"));
		saveExternalForTest(cafeSnapshot("222222222", "Bakery Cafe"));
		saveExternalForTest(noTaxonomySnapshot("333333333", "Activity Place"));

		MyRoomPlacePageResult foodPlaces = roomPlaceQueryService.searchMyRoomPlaces(
				USER_ID,
				null,
				"FOOD",
				"ALL",
				null,
				null,
				0,
				10,
				null
		);
		MyRoomPlacePageResult cafePlaces = roomPlaceQueryService.searchMyRoomPlaces(
				USER_ID,
				"Bakery",
				"CAFE",
				null,
				null,
				null,
				0,
				10,
				null
		);
		MyRoomPlacePageResult firstPage = roomPlaceQueryService.searchMyRoomPlaces(
				USER_ID,
				null,
				null,
				null,
				null,
				null,
				0,
				1,
				null
		);

		assertThat(foodPlaces.items()).extracting(item -> item.place().name()).containsExactly("Donkatsu Place");
		assertThat(cafePlaces.items()).extracting(item -> item.place().name()).containsExactly("Bakery Cafe");
		assertThat(firstPage.items()).hasSize(1);
		assertThat(firstPage.totalElements()).isEqualTo(3);
		assertThat(firstPage.totalPages()).isEqualTo(3);
	}

	@Test
	void shouldFillMyRoomPlaceBusinessHoursStatusFromBulkCache() {
		saveExternalForTest(foodSnapshot("123456789", "Business Hours Place"));
		PlaceBusinessHours cache = PlaceBusinessHours.create(
				"123456789",
				"https://place.map.kakao.com/123456789",
				"Business Hours Place"
		);
		Instant fetchedAt = Instant.parse("2026-05-14T02:00:00Z");
		Instant expiresAt = Instant.parse("2026-05-15T02:00:00Z");
		cache.applyRemotePlace(
				"https://place.map.kakao.com/123456789",
				"Business Hours Place",
				null,
				null,
				BusinessHoursStatus.SUCCEEDED,
				fetchedAt,
				expiresAt,
				"test",
				"job-123",
				null,
				null
		);
		placeBusinessHoursRepository.saveAndFlush(cache);

		MyRoomPlacePageResult result = roomPlaceQueryService.searchMyRoomPlaces(
				USER_ID,
				null,
				null,
				null,
				null,
				null,
				0,
				20,
				null
		);

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).place().businessHoursStatus()).isEqualTo("SUCCEEDED");
		assertThat(result.items().get(0).place().businessHoursFetchedAt()).isEqualTo(fetchedAt);
		assertThat(result.items().get(0).place().businessHoursExpiresAt()).isEqualTo(expiresAt);
	}

	private RoomPlaceSaveResult saveExternalForTest(PlaceSnapshot snapshot) {
		return saveExternalForTest(USER_ID, snapshot);
	}

	private RoomPlaceSaveResult saveExternalForTest(Long userId, PlaceSnapshot snapshot) {
		return saveExternalForTest(room, userId, snapshot);
	}

	private RoomPlaceSaveResult saveExternalForTest(Room targetRoom, Long userId, PlaceSnapshot snapshot) {
		return transactionTemplate.execute(status -> new RoomPlaceSaveResult(
				null,
				roomPlaceStorageService.saveAll(
						roomRepository.findById(targetRoom.getId()).orElseThrow(),
						userId,
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
				"02-000-0000",
				"서울시 종로구",
				"종로 1",
				new BigDecimal("126.972000000000"),
				new BigDecimal("37.570000000000"),
				"https://place.map.kakao.com/" + kakaoPlaceId
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
				null
		);
	}

	private static PlaceSnapshot cafeSnapshot(String kakaoPlaceId, String name) {
		return PlaceSnapshot.kakao(
				kakaoPlaceId,
				name,
				"음식점 > 카페 > 베이커리",
				"CE7",
				"02-111-1111",
				"서울시 종로구",
				"종로 2",
				new BigDecimal("126.973000000000"),
				new BigDecimal("37.571000000000"),
				"https://place.map.kakao.com/" + kakaoPlaceId
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
				address,
				address,
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
