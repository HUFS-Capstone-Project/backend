package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.external.kakao.KakaoLocalClient;
import com.hufs.capstone.backend.place.application.dto.ExternalPlaceCandidateSearchQuery;
import com.hufs.capstone.backend.place.application.dto.ExternalPlaceCandidateSearchResult;
import com.hufs.capstone.backend.place.application.dto.PlaceCandidatePageResult;
import com.hufs.capstone.backend.place.application.dto.PlaceCandidateResult;
import com.hufs.capstone.backend.place.application.dto.ResolvedPlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.enums.PlaceSource;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceCandidateQueryService {

	private final RoomAccessService roomAccessService;
	private final KakaoLocalClient kakaoLocalClient;
	private final RoomPlaceRepository roomPlaceRepository;
	private final PlaceTaxonomyReadService placeTaxonomyReadService;

	public List<PlaceCandidateResult> searchExternalCandidates(
			Long userId,
			String roomId,
			ExternalPlaceCandidateSearchQuery query
	) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		List<PlaceSnapshot> candidates = kakaoLocalClient.searchByKeyword(query);
		Map<String, RoomPlace> existingByExternalPlaceId = findExistingRoomPlaces(room.getId(), candidates).stream()
				.collect(Collectors.toMap(
						roomPlace -> roomPlace.getPlace().getExternalPlaceId(),
						Function.identity(),
						(first, ignored) -> first
				));
		return candidates.stream()
				.map(candidate -> toCandidateResult(candidate, existingByExternalPlaceId))
				.toList();
	}

	public PlaceCandidatePageResult searchExternalCandidatePage(
			Long userId,
			String roomId,
			ExternalPlaceCandidateSearchQuery query
	) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		ExternalPlaceCandidateSearchResult result = kakaoLocalClient.searchByKeywordPage(query);
		Map<String, RoomPlace> existingByExternalPlaceId = findExistingRoomPlaces(room.getId(), result.items()).stream()
				.collect(Collectors.toMap(
						roomPlace -> roomPlace.getPlace().getExternalPlaceId(),
						Function.identity(),
						(first, ignored) -> first
				));
		List<PlaceCandidateResult> items = result.items().stream()
				.map(candidate -> toCandidateResult(candidate, existingByExternalPlaceId))
				.toList();
		return new PlaceCandidatePageResult(
				items,
				result.page(),
				result.limit(),
				result.hasNext(),
				result.nextPage(),
				result.totalCount(),
				result.pageableCount()
		);
	}

	private List<RoomPlace> findExistingRoomPlaces(Long roomId, List<PlaceSnapshot> candidates) {
		List<String> externalPlaceIds = candidates.stream()
				.map(PlaceSnapshot::externalPlaceId)
				.filter(value -> value != null && !value.isBlank())
				.map(String::trim)
				.distinct()
				.toList();
		if (externalPlaceIds.isEmpty()) {
			return List.of();
		}
		return roomPlaceRepository.findExistingByRoomIdAndSourceExternalPlaceIds(
				roomId,
				PlaceSource.KAKAO,
				externalPlaceIds
		);
	}

	private PlaceCandidateResult toCandidateResult(
			PlaceSnapshot candidate,
			Map<String, RoomPlace> existingByExternalPlaceId
	) {
		ResolvedPlaceCategory serviceCategory = placeTaxonomyReadService.resolveCategory(
				candidate.categoryGroupCode(),
				candidate.categoryName()
		);
		if (!candidate.hasKakaoPlaceId()) {
			return PlaceCandidateResult.missingKakaoPlaceId(candidate, serviceCategory);
		}
		RoomPlace existing = existingByExternalPlaceId.get(candidate.externalPlaceId());
		if (existing != null) {
			return PlaceCandidateResult.alreadyInRoom(candidate, serviceCategory, existing.getId());
		}
		return PlaceCandidateResult.selectable(candidate, serviceCategory);
	}
}
