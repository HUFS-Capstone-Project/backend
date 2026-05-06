package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.place.application.dto.ResolvedPlaceTaxonomy;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceSaveResult.SavedRoomPlaceResult;
import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.entity.RoomPlaceSource;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceSourceType;
import com.hufs.capstone.backend.place.domain.repository.PlaceBusinessHoursRepository;
import com.hufs.capstone.backend.place.domain.repository.PlaceRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceSourceRepository;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import com.hufs.capstone.backend.region.application.RegionAddressResolver;
import com.hufs.capstone.backend.region.application.dto.ResolvedRegion;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomPlaceStorageService {

	private final PlaceRepository placeRepository;
	private final RoomPlaceRepository roomPlaceRepository;
	private final RoomPlaceSourceRepository roomPlaceSourceRepository;
	private final PlaceTaxonomyResolver placeTaxonomyResolver;
	private final RegionAddressResolver regionAddressResolver;
	private final PlaceBusinessHoursRepository placeBusinessHoursRepository;
	private final PlaceBusinessHoursRefreshPolicy placeBusinessHoursRefreshPolicy;
	private final ApplicationEventPublisher eventPublisher;

	public List<SavedRoomPlaceResult> saveAll(
			Room room,
			Long userId,
			List<PlaceSnapshot> snapshots,
			String memo,
			RoomPlaceSourceType sourceType,
			RoomLink sourceRoomLink
	) {
		List<SavedRoomPlaceResult> results = new ArrayList<>(snapshots.size());
		try {
			for (PlaceSnapshot snapshot : snapshots) {
				results.add(saveOne(room, userId, snapshot, memo, sourceType, sourceRoomLink));
			}
			roomPlaceRepository.flush();
			roomPlaceSourceRepository.flush();
			return List.copyOf(results);
		} catch (DataIntegrityViolationException ex) {
			throw new RoomPlaceDuplicateRaceException(ex);
		}
	}

	public SavedRoomPlaceResult saveOne(
			Room room,
			Long userId,
			PlaceSnapshot snapshot,
			String memo,
			RoomPlaceSourceType sourceType,
			RoomLink sourceRoomLink
	) {
		validateSnapshot(snapshot);
		Place place = upsertPlace(snapshot);
		ResolvedRegion region = regionAddressResolver.resolve(snapshot.address(), snapshot.roadAddress());
		RoomPlace existingRoomPlace = roomPlaceRepository.findByRoomIdAndPlaceId(room.getId(), place.getId())
				.orElse(null);
		if (existingRoomPlace != null) {
			existingRoomPlace.fillRegionIfAbsent(region);
			publishBusinessHoursRequestIfNeeded(existingRoomPlace, false);
			return toResult(existingRoomPlace, false, true);
		}
		RoomPlace roomPlace = RoomPlace.create(room, place, userId, memo, sourceType, sourceRoomLink, snapshot, region);
		RoomPlace saved = roomPlaceRepository.save(roomPlace);
		attachSourceIfNeeded(saved, sourceRoomLink, sourceType, userId, snapshot);
		publishBusinessHoursRequestIfNeeded(saved, true);
		return toResult(saved, true, false);
	}

	private void publishBusinessHoursRequestIfNeeded(RoomPlace roomPlace, boolean created) {
		PlaceBusinessHours cache = placeBusinessHoursRepository.findByKakaoPlaceId(roomPlace.getKakaoPlaceId())
				.orElse(null);
		if (!placeBusinessHoursRefreshPolicy.shouldRequest(cache, Instant.now())) {
			return;
		}
		Place place = roomPlace.getPlace();
		eventPublisher.publishEvent(new BusinessHoursRequestedEvent(
				roomPlace.getId(),
				place.getId(),
				place.getKakaoPlaceId(),
				place.getPlaceUrl(),
				place.getName(),
				created,
				refreshReason(created, cache)
		));
	}

	private static String refreshReason(boolean created, PlaceBusinessHours cache) {
		if (created) {
			return "created";
		}
		if (cache == null) {
			return "missing_cache";
		}
		if (cache.getBusinessHoursExpiresAt() == null) {
			return "null_expires_at";
		}
		return "expired";
	}

	private void attachSourceIfNeeded(
			RoomPlace roomPlace,
			RoomLink sourceRoomLink,
			RoomPlaceSourceType sourceType,
			Long userId,
			PlaceSnapshot snapshot
	) {
		if (sourceRoomLink == null) {
			return;
		}
		roomPlace.fillSourceRoomLinkIfAbsent(sourceRoomLink);
		if (roomPlace.getId() == null || sourceRoomLink.getId() == null) {
			roomPlaceRepository.flush();
		}
		if (roomPlaceSourceRepository.existsByRoomPlaceIdAndRoomLinkId(roomPlace.getId(), sourceRoomLink.getId())) {
			return;
		}
		roomPlaceSourceRepository.save(RoomPlaceSource.create(roomPlace, sourceRoomLink, sourceType, userId, snapshot));
	}

	private Place upsertPlace(PlaceSnapshot snapshot) {
		ResolvedPlaceTaxonomy taxonomy = null;
		String externalPlaceId = snapshot.externalPlaceId().trim();
		Place existing = placeRepository.findBySourceAndExternalPlaceId(snapshot.source(), externalPlaceId)
				.orElse(null);
		if (existing == null || snapshot.hasTaxonomySignal()) {
			taxonomy = placeTaxonomyResolver.resolve(snapshot.categoryGroupCode(), snapshot.categoryName());
		}
		if (existing == null) {
			Place place = Place.create(snapshot, taxonomy.category(), taxonomy.tag());
			return placeRepository.save(place);
		}
		if (taxonomy == null) {
			existing.updateFrom(snapshot, null, null);
		} else {
			existing.updateFrom(snapshot, taxonomy.category(), taxonomy.tag());
		}
		return existing;
	}

	private static void validateSnapshot(PlaceSnapshot snapshot) {
		if (snapshot == null || snapshot.source() == null) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "Place snapshot is required.");
		}
		if (snapshot.externalPlaceId() == null || snapshot.externalPlaceId().isBlank()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "externalPlaceId is required.");
		}
		if (!snapshot.hasKakaoPlaceId()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "kakaoPlaceId is required.");
		}
	}

	private static SavedRoomPlaceResult toResult(RoomPlace roomPlace, boolean created, boolean alreadyInRoom) {
		return new SavedRoomPlaceResult(
				roomPlace.getId(),
				roomPlace.getPlaceId(),
				roomPlace.getKakaoPlaceId(),
				roomPlace.getPlaceName(),
				created,
				alreadyInRoom
		);
	}
}
