package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.RoomPlaceMemo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RoomPlaceMemoRepository extends JpaRepository<RoomPlaceMemo, Long> {

	@Query("""
			select rpm
			from RoomPlaceMemo rpm
			where rpm.roomPlace.id = :roomPlaceId
			  and rpm.userId = :userId
			""")
	Optional<RoomPlaceMemo> findByRoomPlaceIdAndUserId(@Param("roomPlaceId") Long roomPlaceId, @Param("userId") Long userId);

	@Query("""
			select rpm
			from RoomPlaceMemo rpm
			where rpm.roomPlace.id in :roomPlaceIds
			order by rpm.updatedAt asc, rpm.id asc
			""")
	List<RoomPlaceMemo> findByRoomPlaceIdInOrderByUpdatedAtAscIdAsc(@Param("roomPlaceIds") Collection<Long> roomPlaceIds);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Transactional
	@Query("""
			delete from RoomPlaceMemo rpm
			where rpm.roomPlace.id = :roomPlaceId
			""")
	int deleteByRoomPlaceId(@Param("roomPlaceId") Long roomPlaceId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Transactional
	@Query("""
			delete from RoomPlaceMemo rpm
			where rpm.roomPlace.id = :roomPlaceId
			  and rpm.userId = :userId
			""")
	int deleteByRoomPlaceIdAndUserId(@Param("roomPlaceId") Long roomPlaceId, @Param("userId") Long userId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Transactional
	@Query("""
			delete from RoomPlaceMemo rpm
			where rpm.roomPlace.room.id = :roomId
			""")
	int deleteByRoomId(@Param("roomId") Long roomId);
}
