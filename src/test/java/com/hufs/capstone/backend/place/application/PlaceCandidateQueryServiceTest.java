package com.hufs.capstone.backend.place.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.external.kakao.KakaoLocalClient;
import com.hufs.capstone.backend.place.application.dto.ExternalPlaceCandidateSearchQuery;
import com.hufs.capstone.backend.place.application.dto.ExternalPlaceCandidateSearchResult;
import com.hufs.capstone.backend.place.application.dto.PlaceCandidatePageResult;
import com.hufs.capstone.backend.place.application.dto.PlaceCandidateResult;
import com.hufs.capstone.backend.place.application.dto.ResolvedPlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.enums.PlaceCandidateDisabledReason;
import com.hufs.capstone.backend.place.domain.enums.PlaceSource;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceAddedVia;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import com.hufs.capstone.backend.region.application.dto.ResolvedRegion;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaceCandidateQueryServiceTest {

	@Mock
	private RoomAccessService roomAccessService;

	@Mock
	private KakaoLocalClient kakaoLocalClient;

	@Mock
	private RoomPlaceRepository roomPlaceRepository;

	@Mock
	private PlaceTaxonomyReadService placeTaxonomyReadService;

	private PlaceCandidateQueryService service;
	private Room room;

	@BeforeEach
	void setUp() {
		service = new PlaceCandidateQueryService(
				roomAccessService,
				kakaoLocalClient,
				roomPlaceRepository,
				placeTaxonomyReadService
		);
		room = Room.create("room-public-id", "Room", "INVITE123", 100L);
		ReflectionTestUtils.setField(room, "id", 1L);
	}

	@Test
	void shouldReturnSelectableStateWithRoomContext() {
		ExternalPlaceCandidateSearchQuery query = ExternalPlaceCandidateSearchQuery.of("카페", null, null, 10);
		PlaceSnapshot existingCandidate = snapshot("123", "이미 저장된 카페");
		PlaceSnapshot newCandidate = snapshot("456", "새 카페");
		PlaceSnapshot missingIdCandidate = snapshot(null, "ID 없는 카페");
		RoomPlace existingRoomPlace = existingRoomPlace(existingCandidate);
		when(roomAccessService.requireMemberRoom("room-public-id", 100L)).thenReturn(room);
		when(kakaoLocalClient.searchByKeyword(query)).thenReturn(List.of(existingCandidate, newCandidate, missingIdCandidate));
		when(placeTaxonomyReadService.resolveCategory(any(), any()))
				.thenReturn(new ResolvedPlaceCategory("CAFE", "카페"));
		when(roomPlaceRepository.findExistingByRoomIdAndSourceExternalPlaceIds(
				eq(1L),
				eq(PlaceSource.KAKAO),
				eq(List.of("123", "456"))
		)).thenReturn(List.of(existingRoomPlace));

		List<PlaceCandidateResult> results = service.searchExternalCandidates(100L, "room-public-id", query);

		assertThat(results).hasSize(3);
		assertThat(results.get(0).alreadyInRoom()).isTrue();
		assertThat(results.get(0).roomPlaceId()).isEqualTo(12L);
		assertThat(results.get(0).selectable()).isFalse();
		assertThat(results.get(0).disabledReason()).isEqualTo(PlaceCandidateDisabledReason.ALREADY_IN_ROOM);
		assertThat(results.get(1).alreadyInRoom()).isFalse();
		assertThat(results.get(1).selectable()).isTrue();
		assertThat(results.get(1).disabledReason()).isNull();
		assertThat(results.get(1).placeUrl()).isEqualTo("https://place.map.kakao.com/456");
		assertThat(results.get(1).serviceCategoryCode()).isEqualTo("CAFE");
		assertThat(results.get(1).serviceCategoryName()).isEqualTo("카페");
		assertThat(results.get(2).selectable()).isFalse();
		assertThat(results.get(2).disabledReason()).isEqualTo(PlaceCandidateDisabledReason.MISSING_KAKAO_PLACE_ID);
	}

	@Test
	void shouldReturnExternalCandidatePageMetadata() {
		ExternalPlaceCandidateSearchQuery query = ExternalPlaceCandidateSearchQuery.of("移댄럹", null, null, 2, 10);
		PlaceSnapshot candidate = snapshot("456", "??移댄럹");
		when(roomAccessService.requireMemberRoom("room-public-id", 100L)).thenReturn(room);
		when(kakaoLocalClient.searchByKeywordPage(query)).thenReturn(new ExternalPlaceCandidateSearchResult(
				List.of(candidate),
				2,
				10,
				true,
				3,
				42,
				40
		));
		when(roomPlaceRepository.findExistingByRoomIdAndSourceExternalPlaceIds(
				eq(1L),
				eq(PlaceSource.KAKAO),
				eq(List.of("456"))
		)).thenReturn(List.of());
		when(placeTaxonomyReadService.resolveCategory(any(), any()))
				.thenReturn(new ResolvedPlaceCategory("CAFE", "移댄럹"));

		PlaceCandidatePageResult result = service.searchExternalCandidatePage(100L, "room-public-id", query);

		assertThat(result.items()).hasSize(1);
		assertThat(result.page()).isEqualTo(2);
		assertThat(result.limit()).isEqualTo(10);
		assertThat(result.hasNext()).isTrue();
		assertThat(result.nextPage()).isEqualTo(3);
		assertThat(result.totalCount()).isEqualTo(42);
		assertThat(result.pageableCount()).isEqualTo(40);
	}

	private static RoomPlace existingRoomPlace(PlaceSnapshot snapshot) {
		PlaceCategory category = PlaceCategory.create("CAFE", "카페", 1, true);
		ReflectionTestUtils.setField(category, "id", 2L);
		PlaceTag tag = PlaceTag.create(category, null, "MISC", "기타", 1, true);
		ReflectionTestUtils.setField(tag, "id", 3L);
		Place place = Place.create(snapshot, category, tag);
		ReflectionTestUtils.setField(place, "id", 10L);
		Room room = Room.create("room-public-id", "Room", "INVITE123", 100L);
		ReflectionTestUtils.setField(room, "id", 1L);
		RoomPlace roomPlace = RoomPlace.create(
				room,
				place,
				100L,
				RoomPlaceAddedVia.EXTERNAL_SEARCH,
				null,
				snapshot,
				ResolvedRegion.unresolved()
		);
		ReflectionTestUtils.setField(roomPlace, "id", 12L);
		return roomPlace;
	}

	private static PlaceSnapshot snapshot(String kakaoPlaceId, String name) {
		return PlaceSnapshot.kakao(
				kakaoPlaceId,
				name,
				"음식점 > 카페",
				"CE7",
				null,
				null,
				null,
				null,
				null,
				kakaoPlaceId == null ? null : "https://place.map.kakao.com/" + kakaoPlaceId
		);
	}
}
