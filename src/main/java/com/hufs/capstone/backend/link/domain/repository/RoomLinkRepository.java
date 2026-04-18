package com.hufs.capstone.backend.link.domain.repository;

import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomLinkRepository extends JpaRepository<RoomLink, Long> {

	boolean existsByRoomAndLinkId(Room room, Long linkId);

	Optional<RoomLink> findByRoomAndLinkId(Room room, Long linkId);

	long countByLinkId(Long linkId);

	long countByRoomIdAndLinkId(Long roomId, Long linkId);

	long countByRoomId(Long roomId);

	long deleteByRoomId(Long roomId);

	@Query("""
			select (count(rl) > 0)
			from RoomLink rl, RoomMember rm
			where rl.link.id = :linkId
			  and rm.userId = :userId
			  and rm.room = rl.room
			""")
	boolean existsAccessibleLinkForUser(@Param("linkId") Long linkId, @Param("userId") Long userId);
}
