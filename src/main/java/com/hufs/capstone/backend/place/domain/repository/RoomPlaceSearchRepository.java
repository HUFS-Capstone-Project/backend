package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.enums.PlaceSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoomPlaceSearchRepository {

	Page<RoomPlace> searchRoomPlaces(
			Long roomId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Long createdBy,
			Pageable pageable
	);

	List<RoomPlace> searchRoomPlacesAfterCursor(
			Long roomId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Long createdBy,
			Instant cursorCreatedAt,
			Long cursorRoomPlaceId,
			int limit
	);

	long countRoomPlaces(
			Long roomId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Long createdBy
	);

	List<RoomPlace> findMapPlacesInBounds(
			Long roomId,
			BigDecimal minLatitude,
			BigDecimal maxLatitude,
			BigDecimal minLongitude,
			BigDecimal maxLongitude,
			Long createdBy,
			int limit
	);

	long countMyRoomPlaces(
			Long userId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode
	);

	Page<RoomPlace> searchMyRoomPlaces(
			Long userId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Pageable pageable
	);

	List<RoomPlace> searchMyRoomPlacesAfterCursor(
			Long userId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Instant cursorCreatedAt,
			Long cursorRoomPlaceId,
			int limit
	);

	List<RoomPlace> findExistingByRoomIdAndKakaoPlaceIds(Long roomId, Collection<String> kakaoPlaceIds);

	List<RoomPlace> findExistingByRoomIdAndSourceExternalPlaceIds(
			Long roomId,
			PlaceSource source,
			Collection<String> externalPlaceIds
	);
}
