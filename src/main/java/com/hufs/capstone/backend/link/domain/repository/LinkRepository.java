package com.hufs.capstone.backend.link.domain.repository;

import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkRepository extends JpaRepository<Link, Long> {

	Optional<Link> findByNormalizedUrl(String normalizedUrl);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Link l
			set l.status = :targetStatus,
			    l.captionRaw = :captionRaw,
			    l.errorCode = :errorCode,
			    l.errorMessage = :errorMessage,
			    l.version = l.version + 1,
			    l.updatedAt = :updatedAt
			where l.id = :linkId
			  and l.version = :expectedVersion
			  and l.status in :updatableStatuses
			""")
	int compareAndSetAnalysisResult(
			@Param("linkId") Long linkId,
			@Param("expectedVersion") Long expectedVersion,
			@Param("updatableStatuses") Collection<LinkAnalysisStatus> updatableStatuses,
			@Param("targetStatus") LinkAnalysisStatus targetStatus,
			@Param("captionRaw") String captionRaw,
			@Param("errorCode") String errorCode,
			@Param("errorMessage") String errorMessage,
			@Param("updatedAt") Instant updatedAt
	);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Link l
			set l.processingJobId = :newProcessingJobId,
			    l.dispatchStatus = :targetDispatchStatus,
			    l.version = l.version + 1,
			    l.updatedAt = :updatedAt
			where l.id = :linkId
			  and l.dispatchStatus = :expectedDispatchStatus
			  and l.processingJobId is null
			""")
	int bindProcessingJobIdForPending(
			@Param("linkId") Long linkId,
			@Param("newProcessingJobId") String newProcessingJobId,
			@Param("expectedDispatchStatus") ProcessingDispatchStatus expectedDispatchStatus,
			@Param("targetDispatchStatus") ProcessingDispatchStatus targetDispatchStatus,
			@Param("updatedAt") Instant updatedAt
	);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Link l
			set l.dispatchStatus = :targetDispatchStatus,
			    l.status = :targetStatus,
			    l.errorCode = :errorCode,
			    l.errorMessage = :errorMessage,
			    l.version = l.version + 1,
			    l.updatedAt = :updatedAt
			where l.id = :linkId
			  and l.dispatchStatus = :expectedDispatchStatus
			  and l.processingJobId is null
			""")
	int markDispatchFailedIfNoJob(
			@Param("linkId") Long linkId,
			@Param("expectedDispatchStatus") ProcessingDispatchStatus expectedDispatchStatus,
			@Param("targetDispatchStatus") ProcessingDispatchStatus targetDispatchStatus,
			@Param("targetStatus") LinkAnalysisStatus targetStatus,
			@Param("errorCode") String errorCode,
			@Param("errorMessage") String errorMessage,
			@Param("updatedAt") Instant updatedAt
	);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Link l
			set l.dispatchStatus = :targetDispatchStatus,
			    l.status = :targetStatus,
			    l.errorCode = null,
			    l.errorMessage = null,
			    l.version = l.version + 1,
			    l.updatedAt = :updatedAt
			where l.id = :linkId
			  and l.dispatchStatus = :expectedDispatchStatus
			  and l.status = :expectedStatus
			  and l.processingJobId is null
			""")
	int recoverDispatchFailedForManualRetry(
			@Param("linkId") Long linkId,
			@Param("expectedDispatchStatus") ProcessingDispatchStatus expectedDispatchStatus,
			@Param("expectedStatus") LinkAnalysisStatus expectedStatus,
			@Param("targetDispatchStatus") ProcessingDispatchStatus targetDispatchStatus,
			@Param("targetStatus") LinkAnalysisStatus targetStatus,
			@Param("updatedAt") Instant updatedAt
	);
}
