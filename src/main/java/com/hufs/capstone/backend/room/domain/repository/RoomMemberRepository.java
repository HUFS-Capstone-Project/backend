package com.hufs.capstone.backend.room.domain.repository;

import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

	boolean existsByRoomAndUserId(Room room, Long userId);

	Optional<RoomMember> findByRoomAndUserId(Room room, Long userId);

	long countByRoomId(Long roomId);

	@Query("""
			select rm.room.id as roomId, count(rm) as count
			from RoomMember rm
			where rm.room.id in :roomIds
			group by rm.room.id
			""")
	List<RoomCountProjection> countByRoomIds(@Param("roomIds") Collection<Long> roomIds);

	List<RoomMember> findByRoomIdOrderByCreatedAtAscIdAsc(Long roomId);

	interface RoomCountProjection {

		Long getRoomId();

		long getCount();
	}
}
