package com.hufs.capstone.backend.link.domain.repository;

import com.hufs.capstone.backend.link.domain.entity.LinkAnalysisRequest;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkAnalysisRequestRepository extends JpaRepository<LinkAnalysisRequest, Long> {

	Optional<LinkAnalysisRequest> findByRoomAndLinkId(Room room, Long linkId);

	boolean existsByRoomAndLinkId(Room room, Long linkId);

	long countByRoomId(Long roomId);

	long countByLinkId(Long linkId);

	long deleteByRoomId(Long roomId);
}
