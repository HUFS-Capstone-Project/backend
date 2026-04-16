package com.hufs.capstone.backend.link.domain.repository;

import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomLinkRepository extends JpaRepository<RoomLink, Long> {

	boolean existsByRoomIdAndLinkId(String roomId, Long linkId);

	Optional<RoomLink> findByRoomIdAndLinkId(String roomId, Long linkId);

	long countByLinkId(Long linkId);

	long countByRoomIdAndLinkId(String roomId, Long linkId);
}
