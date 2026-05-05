package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoomPlaceSearchRepository {

	Page<RoomPlace> searchRoomPlaces(
			Long roomId,
			String keyword,
			String initialKeyword,
			String categoryCode,
			String tagCode,
			Pageable pageable
	);
}
