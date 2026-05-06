package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.application.dto.ProcessingResultSnapshot;
import com.hufs.capstone.backend.link.application.event.LinkStatusSyncedEvent;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkAnalysisStatusWriteService {

	private static final Set<LinkAnalysisStatus> UPDATABLE_STATUSES =
			Set.of(LinkAnalysisStatus.REQUESTED, LinkAnalysisStatus.PROCESSING);
	private static final int MAX_CAS_RETRY = 3;

	private final LinkRepository linkRepository;
	private final LinkAnalysisResultMapper linkAnalysisResultMapper;
	private final LinkPlaceCandidateSnapshotMapper placeCandidateSnapshotMapper;
	private final LinkCandidateSyncService linkCandidateSyncService;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public LinkAnalysisResult applySyncSnapshot(
			Long linkId,
			LinkAnalysisStatus targetStatus,
			ProcessingResultSnapshot result,
			String errorCode,
			String errorMessage
	) {
		for (int retry = 0; retry < MAX_CAS_RETRY; retry++) {
			Link current = linkRepository.findById(linkId)
					.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "Link not found."));

			if (current.isTerminal()) {
				return linkAnalysisResultMapper.from(current);
			}

			CasPlan plan = CasPlan.from(current, targetStatus, result, errorCode, errorMessage);
			if (!plan.changed()) {
				return linkAnalysisResultMapper.from(current);
			}

			int updated = executeCasUpdate(current.getId(), current.getVersion(), plan);
			if (updated == 1) {
				Link refreshed = linkRepository.findById(linkId)
						.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "Link not found."));
				if (plan.targetStatus() == LinkAnalysisStatus.SUCCEEDED) {
					linkCandidateSyncService.replaceCandidates(
							refreshed,
							placeCandidateSnapshotMapper.read(refreshed.getExtractedPlacesJson())
					);
				}
				eventPublisher.publishEvent(new LinkStatusSyncedEvent(refreshed.getId()));
				return linkAnalysisResultMapper.from(refreshed);
			}
		}

		log.warn("CAS update conflict. Returning latest link analysis status. linkId={}, targetStatus={}", linkId, targetStatus);
		Link latest = linkRepository.findById(linkId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "Link not found."));
		return linkAnalysisResultMapper.from(latest);
	}

	private int executeCasUpdate(Long linkId, Long expectedVersion, CasPlan plan) {
		ProcessingResultSnapshot result = plan.result();
		return linkRepository.compareAndSetAnalysisResult(
				linkId,
				expectedVersion,
				UPDATABLE_STATUSES,
				plan.targetStatus(),
				result.captionRaw(),
				result.extractionStoreName(),
				result.extractionAddress(),
				result.extractionCertainty(),
				result.extractedPlacesJson(),
				result.processingResultJson(),
				plan.errorCode(),
				plan.errorMessage(),
				Instant.now()
		);
	}

	private record CasPlan(
			boolean changed,
			LinkAnalysisStatus targetStatus,
			ProcessingResultSnapshot result,
			String errorCode,
			String errorMessage
	) {

		private static CasPlan from(
				Link current,
				LinkAnalysisStatus targetStatus,
				ProcessingResultSnapshot result,
				String errorCode,
				String errorMessage
		) {
			return switch (targetStatus) {
				case REQUESTED -> requestedPlan(current);
				case PROCESSING -> processingPlan(current);
				case FAILED -> failedPlan(current, errorCode, errorMessage);
				case DISPATCH_FAILED -> dispatchFailedPlan(current, errorCode, errorMessage);
				case SUCCEEDED -> succeededPlan(result);
			};
		}

		private static CasPlan requestedPlan(Link current) {
			if (current.getStatus() == LinkAnalysisStatus.REQUESTED || current.getStatus() == LinkAnalysisStatus.PROCESSING) {
				return unchanged(current.getStatus(), ProcessingResultSnapshot.empty(), current.getErrorCode(),
						current.getErrorMessage());
			}
			return changed(LinkAnalysisStatus.REQUESTED, ProcessingResultSnapshot.empty(), null, null);
		}

		private static CasPlan processingPlan(Link current) {
			if (current.getStatus() == LinkAnalysisStatus.PROCESSING) {
				return unchanged(current.getStatus(), ProcessingResultSnapshot.empty(), current.getErrorCode(),
						current.getErrorMessage());
			}
			return changed(LinkAnalysisStatus.PROCESSING, ProcessingResultSnapshot.empty(), null, null);
		}

		private static CasPlan failedPlan(Link current, String errorCode, String errorMessage) {
			if (current.getStatus().isTerminal()) {
				return unchanged(current.getStatus(), ProcessingResultSnapshot.empty(), current.getErrorCode(),
						current.getErrorMessage());
			}
			return changed(LinkAnalysisStatus.FAILED, ProcessingResultSnapshot.empty(), errorCode, errorMessage);
		}

		private static CasPlan dispatchFailedPlan(Link current, String errorCode, String errorMessage) {
			if (current.getStatus().isTerminal()) {
				return unchanged(current.getStatus(), ProcessingResultSnapshot.empty(), current.getErrorCode(),
						current.getErrorMessage());
			}
			return changed(LinkAnalysisStatus.DISPATCH_FAILED, ProcessingResultSnapshot.empty(), errorCode, errorMessage);
		}

		private static CasPlan succeededPlan(ProcessingResultSnapshot result) {
			return changed(LinkAnalysisStatus.SUCCEEDED, result == null ? ProcessingResultSnapshot.empty() : result, null, null);
		}

		private static CasPlan unchanged(
				LinkAnalysisStatus status,
				ProcessingResultSnapshot result,
				String errorCode,
				String errorMessage
		) {
			return new CasPlan(false, status, result, errorCode, errorMessage);
		}

		private static CasPlan changed(
				LinkAnalysisStatus status,
				ProcessingResultSnapshot result,
				String errorCode,
				String errorMessage
		) {
			return new CasPlan(true, status, result, errorCode, errorMessage);
		}
	}
}
