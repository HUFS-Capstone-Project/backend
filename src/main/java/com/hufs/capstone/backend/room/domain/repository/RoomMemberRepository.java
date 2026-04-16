package com.hufs.capstone.backend.room.domain.repository;

import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

	boolean existsByRoomIdAndUserId(Long roomId, Long userId);

	boolean existsByRoomAndUserId(Room room, Long userId);

	Optional<RoomMember> findByRoomAndUserId(Room room, Long userId);

	long countByRoomId(Long roomId);

	@EntityGraph(attributePaths = "room")
	List<RoomMember> findByUserIdOrderByCreatedAtDesc(Long userId);
}

