package com.hufs.capstone.backend.link.domain.repository;

import com.hufs.capstone.backend.link.domain.entity.RoomLinkCandidateOverride;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RoomLinkCandidateOverrideRepository extends JpaRepository<RoomLinkCandidateOverride, Long> {

	@EntityGraph(attributePaths = {"linkCandidate", "roomLink"})
	List<RoomLinkCandidateOverride> findByRoomLinkId(Long roomLinkId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"linkCandidate", "roomLink"})
	@Query("""
			select override
			from RoomLinkCandidateOverride override
			where override.roomLink.id = :roomLinkId
			  and override.linkCandidate.id = :linkCandidateId
			""")
	Optional<RoomLinkCandidateOverride> findByRoomLinkIdAndLinkCandidateIdForUpdate(
			@Param("roomLinkId") Long roomLinkId,
			@Param("linkCandidateId") Long linkCandidateId
	);
}
