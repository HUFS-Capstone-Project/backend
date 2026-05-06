package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RoomPlaceRepository extends JpaRepository<RoomPlace, Long>, RoomPlaceSearchRepository {

	@Query("""
			select rp
			from RoomPlace rp
			where rp.room.id = :roomId
			  and rp.place.id = :placeId
			""")
	Optional<RoomPlace> findByRoomIdAndPlaceId(@Param("roomId") Long roomId, @Param("placeId") Long placeId);

	@Query("""
			select rp
			from RoomPlace rp
			join fetch rp.place p
			join fetch p.serviceCategory c
			join fetch p.serviceTag t
			left join fetch rp.sourceRoomLink srl
			left join fetch srl.link
			where rp.id = :id
			  and rp.room.id = :roomId
			""")
	Optional<RoomPlace> findByIdAndRoomId(@Param("id") Long id, @Param("roomId") Long roomId);

	@Query("""
			select count(rp)
			from RoomPlace rp
			join rp.place p
			where rp.room.id = :roomId
			  and p.kakaoPlaceId = :kakaoPlaceId
			""")
	long countByRoomIdAndKakaoPlaceId(@Param("roomId") Long roomId, @Param("kakaoPlaceId") String kakaoPlaceId);

	long countByRoomId(Long roomId);

	long deleteByRoomId(Long roomId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Transactional
	@Query("""
			update RoomPlace rp
			set rp.sourceRoomLink = null
			where rp.sourceRoomLink.id = :roomLinkId
			""")
	int clearSourceRoomLinkBySourceRoomLinkId(@Param("roomLinkId") Long roomLinkId);

}
