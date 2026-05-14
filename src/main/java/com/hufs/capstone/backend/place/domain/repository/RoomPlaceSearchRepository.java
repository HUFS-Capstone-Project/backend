package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.enums.PlaceSource;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoomPlaceSearchRepository {

	Page<RoomPlace> searchRoomPlaces(
			Long roomId,
			String keyword,
			String initialKeyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Long createdBy,
			Pageable pageable
	);

	Page<RoomPlace> searchMyRoomPlaces(
			Long userId,
			String keyword,
			String initialKeyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Pageable pageable
	);

	List<RoomPlace> findExistingByRoomIdAndKakaoPlaceIds(Long roomId, Collection<String> kakaoPlaceIds);

	List<RoomPlace> findExistingByRoomIdAndSourceExternalPlaceIds(
			Long roomId,
			PlaceSource source,
			Collection<String> externalPlaceIds
	);
}
