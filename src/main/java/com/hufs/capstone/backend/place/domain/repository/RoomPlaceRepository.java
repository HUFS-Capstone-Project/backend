package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.util.Collection;
import java.util.List;
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
			left join fetch rp.originRoomLink orl
			left join fetch orl.link
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

	@Query("""
			select rp.room.id as roomId, count(rp) as count
			from RoomPlace rp
			where rp.room.id in :roomIds
			group by rp.room.id
			""")
	List<RoomCountProjection> countByRoomIds(@Param("roomIds") Collection<Long> roomIds);

	@Query("""
			select distinct rp.sidoCode as code, rp.sidoName as name
			from RoomPlace rp
			where rp.room.id = :roomId
			  and rp.sidoCode is not null
			  and rp.sidoCode <> ''
			order by rp.sidoCode asc
			""")
	List<RoomPlaceRegionOption> findDistinctSidoOptionsByRoomId(@Param("roomId") Long roomId);

	@Query("""
			select distinct rp.sigunguCode as code, rp.sigunguName as name
			from RoomPlace rp
			where rp.room.id = :roomId
			  and rp.sidoCode = :sidoCode
			  and rp.sigunguCode is not null
			  and rp.sigunguCode <> ''
			order by rp.sigunguCode asc
			""")
	List<RoomPlaceRegionOption> findDistinctSigunguOptionsByRoomIdAndSidoCode(
			@Param("roomId") Long roomId,
			@Param("sidoCode") String sidoCode
	);

	@Query("""
			select case when count(rp) > 0 then true else false end
			from RoomPlace rp
			where rp.room.id = :roomId
			  and rp.sidoCode = :sidoCode
			""")
	boolean existsByRoomIdAndSidoCode(@Param("roomId") Long roomId, @Param("sidoCode") String sidoCode);

	/**
	 * 코스 수정 시 roomPlaceId 목록이 모두 해당 방에 속하는지 검증하고 엔티티를 확보한다.
	 * place/serviceCategory/serviceTag를 fetchJoin으로 즉시 로드한다.
	 * deleteByDateCourseId의 clearAutomatically=true 이후에도 detached 상태에서 접근 가능하게 하기 위함.
	 */
	@Query("""
			select rp from RoomPlace rp
			join fetch rp.place p
			join fetch p.serviceCategory
			join fetch p.serviceTag
			left join fetch rp.originRoomLink orl
			left join fetch orl.link
			where rp.id in :ids and rp.room.id = :roomId
			""")
	List<RoomPlace> findAllByIdInAndRoomId(@Param("ids") Collection<Long> ids, @Param("roomId") Long roomId);

	long deleteByRoomId(Long roomId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Transactional
	@Query("""
			update RoomPlace rp
			set rp.originRoomLink = null
			where rp.originRoomLink.id = :roomLinkId
			""")
	int clearOriginRoomLinkByOriginRoomLinkId(@Param("roomLinkId") Long roomLinkId);

	interface RoomPlaceRegionOption {

		String getCode();

		String getName();
	}

	interface RoomCountProjection {

		Long getRoomId();

		long getCount();
	}
}
