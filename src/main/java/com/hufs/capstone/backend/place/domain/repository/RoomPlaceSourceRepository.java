package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.RoomPlaceSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RoomPlaceSourceRepository extends JpaRepository<RoomPlaceSource, Long> {

	boolean existsByRoomPlaceIdAndRoomLinkId(Long roomPlaceId, Long roomLinkId);

	long countByRoomLinkId(Long roomLinkId);

	long countByRoomPlaceId(Long roomPlaceId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Transactional
	@Query("""
			delete from RoomPlaceSource rps
			where rps.roomPlace.id = :roomPlaceId
			""")
	int deleteByRoomPlaceId(@Param("roomPlaceId") Long roomPlaceId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Transactional
	@Query("""
			delete from RoomPlaceSource rps
			where rps.roomPlace.room.id = :roomId
			""")
	int deleteByRoomId(@Param("roomId") Long roomId);
}
