package com.hufs.capstone.backend.room.domain.repository;

import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long>, RoomMemberSearchRepository {

	boolean existsByRoomAndUserId(Room room, Long userId);

	Optional<RoomMember> findByRoomAndUserId(Room room, Long userId);

	long countByRoomId(Long roomId);

}

