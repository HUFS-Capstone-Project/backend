package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.application.event.LinkStatusSyncedEvent;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import java.time.Instant;
import java.util.Objects;
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
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public LinkAnalysisResult applySyncSnapshot(
			Long linkId,
			LinkAnalysisStatus targetStatus,
			String captionRaw,
			String errorCode,
			String errorMessage
	) {
		for (int retry = 0; retry < MAX_CAS_RETRY; retry++) {
			Link current = linkRepository.findById(linkId)
					.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "링크를 찾을 수 없습니다."));

			if (current.isTerminal()) {
				return LinkAnalysisResult.from(current);
			}

			CasPlan plan = CasPlan.from(current, targetStatus, captionRaw, errorCode, errorMessage);
			if (!plan.changed()) {
				return LinkAnalysisResult.from(current);
			}

			int updated = executeCasUpdate(current.getId(), current.getVersion(), plan);
			if (updated == 1) {
				Link refreshed = linkRepository.findById(linkId)
						.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "링크를 찾을 수 없습니다."));
				eventPublisher.publishEvent(new LinkStatusSyncedEvent(refreshed.getId()));
				return LinkAnalysisResult.from(refreshed);
			}
		}

		log.warn("CAS 업데이트 경합이 발생해 최신 링크 분석 상태를 반환합니다. linkId={}, targetStatus={}", linkId, targetStatus);
		Link latest = linkRepository.findById(linkId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "링크를 찾을 수 없습니다."));
		return LinkAnalysisResult.from(latest);
	}

	private int executeCasUpdate(Long linkId, Long expectedVersion, CasPlan plan) {
		return linkRepository.compareAndSetAnalysisResult(
				linkId,
				expectedVersion,
				UPDATABLE_STATUSES,
				plan.targetStatus(),
				plan.captionRaw(),
				plan.errorCode(),
				plan.errorMessage(),
				Instant.now()
		);
	}

	private record CasPlan(
			boolean changed,
			LinkAnalysisStatus targetStatus,
			String captionRaw,
			String errorCode,
			String errorMessage
	) {

		private static CasPlan from(
				Link current,
				LinkAnalysisStatus targetStatus,
				String captionRaw,
				String errorCode,
				String errorMessage
		) {
			return switch (targetStatus) {
				case REQUESTED -> requestedPlan(current);
				case PROCESSING -> processingPlan(current);
				case FAILED -> failedPlan(current, errorCode, errorMessage);
				case DISPATCH_FAILED -> dispatchFailedPlan(current, errorCode, errorMessage);
				case SUCCEEDED -> succeededPlan(current, captionRaw);
			};
		}

		private static CasPlan requestedPlan(Link current) {
			if (current.getStatus() == LinkAnalysisStatus.REQUESTED || current.getStatus() == LinkAnalysisStatus.PROCESSING) {
				return unchanged(current.getStatus(), current.getCaptionRaw(), current.getErrorCode(), current.getErrorMessage());
			}
			return changed(LinkAnalysisStatus.REQUESTED, null, null, null);
		}

		private static CasPlan processingPlan(Link current) {
			if (current.getStatus() == LinkAnalysisStatus.PROCESSING) {
				return unchanged(current.getStatus(), current.getCaptionRaw(), current.getErrorCode(), current.getErrorMessage());
			}
			return changed(LinkAnalysisStatus.PROCESSING, null, null, null);
		}

		private static CasPlan failedPlan(Link current, String errorCode, String errorMessage) {
			if (current.getStatus().isTerminal()) {
				return unchanged(current.getStatus(), current.getCaptionRaw(), current.getErrorCode(), current.getErrorMessage());
			}
			return changed(LinkAnalysisStatus.FAILED, null, errorCode, errorMessage);
		}

		private static CasPlan dispatchFailedPlan(Link current, String errorCode, String errorMessage) {
			if (current.getStatus().isTerminal()) {
				return unchanged(current.getStatus(), current.getCaptionRaw(), current.getErrorCode(), current.getErrorMessage());
			}
			return changed(LinkAnalysisStatus.DISPATCH_FAILED, null, errorCode, errorMessage);
		}

		private static CasPlan succeededPlan(Link current, String captionRaw) {
			boolean changedStatus = current.getStatus() != LinkAnalysisStatus.SUCCEEDED;
			boolean changedCaption = !Objects.equals(current.getCaptionRaw(), captionRaw);
			boolean changedError = current.getErrorCode() != null || current.getErrorMessage() != null;
			if (!changedStatus && !changedCaption && !changedError) {
				return unchanged(current.getStatus(), current.getCaptionRaw(), current.getErrorCode(), current.getErrorMessage());
			}
			return changed(LinkAnalysisStatus.SUCCEEDED, captionRaw, null, null);
		}

		private static CasPlan unchanged(
				LinkAnalysisStatus status,
				String captionRaw,
				String errorCode,
				String errorMessage
		) {
			return new CasPlan(false, status, captionRaw, errorCode, errorMessage);
		}

		private static CasPlan changed(
				LinkAnalysisStatus status,
				String captionRaw,
				String errorCode,
				String errorMessage
		) {
			return new CasPlan(true, status, captionRaw, errorCode, errorMessage);
		}
	}
}
