package com.hufs.capstone.backend.room.domain.repository;

import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

	boolean existsByRoomIdAndUserId(Long roomId, Long userId);

	boolean existsByRoomAndUserId(Room room, Long userId);

	Optional<RoomMember> findByRoomAndUserId(Room room, Long userId);

	long countByRoomId(Long roomId);

	@Query("""
			select rm
			from RoomMember rm
			join fetch rm.room r
			where rm.userId = :userId
			order by rm.pinned desc, r.createdAt desc
			""")
	List<RoomMember> findMyRooms(@Param("userId") Long userId);
}

