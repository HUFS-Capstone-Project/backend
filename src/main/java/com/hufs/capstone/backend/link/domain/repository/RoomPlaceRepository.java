package com.hufs.capstone.backend.link.domain.repository;

import com.hufs.capstone.backend.link.domain.entity.RoomPlace;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomPlaceRepository extends JpaRepository<RoomPlace, Long> {

	List<RoomPlace> findByRoomIdAndKakaoPlaceIdIn(Long roomId, Collection<String> kakaoPlaceIds);

	long countByRoomIdAndKakaoPlaceId(Long roomId, String kakaoPlaceId);

	long countByRoomId(Long roomId);

	long deleteByRoomId(Long roomId);
}
