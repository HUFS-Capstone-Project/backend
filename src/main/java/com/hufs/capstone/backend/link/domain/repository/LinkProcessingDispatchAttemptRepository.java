package com.hufs.capstone.backend.link.domain.repository;

import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.LinkProcessingDispatchAttemptStatus;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
import com.hufs.capstone.backend.link.domain.entity.LinkProcessingDispatchAttempt;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkProcessingDispatchAttemptRepository
		extends JpaRepository<LinkProcessingDispatchAttempt, Long> {

	long countByLinkId(Long linkId);

	long countByLinkIdAndActiveSlotIsNotNull(Long linkId);

	@EntityGraph(attributePaths = "link")
	@Query("select attempt from LinkProcessingDispatchAttempt attempt "
			+ "where attempt.link.id = :linkId and attempt.activeSlot = 1")
	Optional<LinkProcessingDispatchAttempt> findActiveByLinkId(@Param("linkId") Long linkId);

	@EntityGraph(attributePaths = "link")
	@Query("select attempt from LinkProcessingDispatchAttempt attempt "
			+ "where attempt.id = :attemptId and attempt.activeSlot = 1")
	Optional<LinkProcessingDispatchAttempt> findActiveById(@Param("attemptId") Long attemptId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select attempt from LinkProcessingDispatchAttempt attempt where attempt.id = :attemptId")
	Optional<LinkProcessingDispatchAttempt> findByIdForUpdate(@Param("attemptId") Long attemptId);

	@EntityGraph(attributePaths = "link")
	@Query("""
			select attempt
			from LinkProcessingDispatchAttempt attempt
			where attempt.activeSlot = 1
			  and (
			      (attempt.status = :pendingStatus and attempt.createdAt <= :staleBefore)
			      or (attempt.status = :dispatchingStatus and attempt.claimedAt <= :staleBefore)
			  )
			  and attempt.link.dispatchStatus in :linkDispatchStatuses
			  and attempt.link.processingJobId is null
			  and attempt.link.status = :linkStatus
			order by coalesce(attempt.claimedAt, attempt.createdAt) asc, attempt.id asc
			""")
	List<LinkProcessingDispatchAttempt> findStaleTargets(
			@Param("pendingStatus") LinkProcessingDispatchAttemptStatus pendingStatus,
			@Param("dispatchingStatus") LinkProcessingDispatchAttemptStatus dispatchingStatus,
			@Param("linkDispatchStatuses") Collection<ProcessingDispatchStatus> linkDispatchStatuses,
			@Param("linkStatus") LinkAnalysisStatus linkStatus,
			@Param("staleBefore") Instant staleBefore,
			Pageable pageable
	);
}
