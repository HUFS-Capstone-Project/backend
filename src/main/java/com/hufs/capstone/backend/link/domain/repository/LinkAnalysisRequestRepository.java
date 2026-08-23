package com.hufs.capstone.backend.link.domain.repository;

import com.hufs.capstone.backend.link.domain.entity.LinkAnalysisRequest;
import com.hufs.capstone.backend.room.domain.entity.Room;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkAnalysisRequestRepository extends JpaRepository<LinkAnalysisRequest, Long> {

	Optional<LinkAnalysisRequest> findByRoomAndLinkId(Room room, Long linkId);

	@EntityGraph(attributePaths = {"room", "link"})
	@Query("""
			select lar
			from LinkAnalysisRequest lar
			where lar.id = :id
			""")
	Optional<LinkAnalysisRequest> findWithRoomAndLinkById(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"room", "link"})
	@Query("""
			select lar
			from LinkAnalysisRequest lar
			where lar.id = :id
			""")
	Optional<LinkAnalysisRequest> findWithRoomAndLinkByIdForUpdate(@Param("id") Long id);

	boolean existsByRoomAndLinkId(Room room, Long linkId);

	long countByRoomId(Long roomId);

	long countByLinkId(Long linkId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from LinkAnalysisRequest lar where lar.room.id = :roomId")
	int deleteByRoomId(@Param("roomId") Long roomId);
}
