package com.hufs.capstone.backend.link.domain.repository;

import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkRepository extends JpaRepository<Link, Long> {

	Optional<Link> findByNormalizedUrl(String normalizedUrl);

	@Query("""
			select l
			from Link l
			where l.dispatchStatus in :dispatchStatuses
			  and l.processingJobId is null
			  and l.status = :status
			  and l.updatedAt <= :staleBefore
			order by l.updatedAt asc, l.id asc
			""")
	List<Link> findStaleDispatchTargets(
			@Param("dispatchStatuses") Collection<ProcessingDispatchStatus> dispatchStatuses,
			@Param("status") LinkAnalysisStatus status,
			@Param("staleBefore") Instant staleBefore,
			Pageable pageable
	);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Link l
			set l.dispatchStatus = :targetDispatchStatus,
			    l.version = l.version + 1,
			    l.updatedAt = :updatedAt
			where l.id = :linkId
			  and l.status = :expectedStatus
			  and l.processingJobId is null
			  and (
			      l.dispatchStatus = :pendingDispatchStatus
			      or (l.dispatchStatus = :dispatchingDispatchStatus and l.updatedAt <= :staleBefore)
			  )
			""")
	int claimDispatchForProcessing(
			@Param("linkId") Long linkId,
			@Param("expectedStatus") LinkAnalysisStatus expectedStatus,
			@Param("pendingDispatchStatus") ProcessingDispatchStatus pendingDispatchStatus,
			@Param("dispatchingDispatchStatus") ProcessingDispatchStatus dispatchingDispatchStatus,
			@Param("targetDispatchStatus") ProcessingDispatchStatus targetDispatchStatus,
			@Param("staleBefore") Instant staleBefore,
			@Param("updatedAt") Instant updatedAt
	);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Link l
			set l.status = :targetStatus,
			    l.contentText = :contentText,
			    l.likeCount = :likeCount,
			    l.commentCount = :commentCount,
			    l.postedAt = :postedAt,
			    l.extractionStoreName = :extractionStoreName,
			    l.extractionAddress = :extractionAddress,
			    l.extractionCertainty = :extractionCertainty,
			    l.extractedPlacesJson = :extractedPlacesJson,
			    l.processingResultJson = :processingResultJson,
			    l.errorCode = :errorCode,
			    l.errorMessage = :errorMessage,
			    l.retryable = :retryable,
			    l.cooldownSeconds = :cooldownSeconds,
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
			@Param("contentText") String contentText,
			@Param("likeCount") Long likeCount,
			@Param("commentCount") Long commentCount,
			@Param("postedAt") String postedAt,
			@Param("extractionStoreName") String extractionStoreName,
			@Param("extractionAddress") String extractionAddress,
			@Param("extractionCertainty") String extractionCertainty,
			@Param("extractedPlacesJson") String extractedPlacesJson,
			@Param("processingResultJson") String processingResultJson,
			@Param("errorCode") String errorCode,
			@Param("errorMessage") String errorMessage,
			@Param("retryable") Boolean retryable,
			@Param("cooldownSeconds") Integer cooldownSeconds,
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
			    l.retryable = :retryable,
			    l.cooldownSeconds = :cooldownSeconds,
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
			@Param("retryable") Boolean retryable,
			@Param("cooldownSeconds") Integer cooldownSeconds,
			@Param("updatedAt") Instant updatedAt
	);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Link l
			set l.dispatchStatus = :targetDispatchStatus,
			    l.status = :targetStatus,
			    l.errorCode = null,
			    l.errorMessage = null,
			    l.retryable = null,
			    l.cooldownSeconds = null,
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
