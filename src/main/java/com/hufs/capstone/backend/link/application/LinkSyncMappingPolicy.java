package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResultResponse;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LinkSyncMappingPolicy {

	public LinkSyncOrchestrator.ProcessingSyncSnapshot pendingSnapshot() {
		return new LinkSyncOrchestrator.ProcessingSyncSnapshot(LinkAnalysisStatus.REQUESTED, null);
	}

	public LinkSyncOrchestrator.ProcessingSyncSnapshot fromObservedStatus(LinkAnalysisStatus observedStatus) {
		return switch (observedStatus) {
			case FAILED -> new LinkSyncOrchestrator.ProcessingSyncSnapshot(LinkAnalysisStatus.FAILED, null);
			case REQUESTED -> new LinkSyncOrchestrator.ProcessingSyncSnapshot(LinkAnalysisStatus.REQUESTED, null);
			case PROCESSING -> new LinkSyncOrchestrator.ProcessingSyncSnapshot(LinkAnalysisStatus.PROCESSING, null);
			case SUCCEEDED -> throw new IllegalArgumentException(
					"SUCCEEDED status requires result payload mapping."
			);
		};
	}

	public LinkSyncOrchestrator.ProcessingSyncSnapshot fromSucceededResult(
			String jobId,
			ProcessingJobResultResponse resultResponse
	) {
		if (resultResponse == null) {
			return new LinkSyncOrchestrator.ProcessingSyncSnapshot(LinkAnalysisStatus.PROCESSING, null);
		}

		String caption = trimToNull(resultResponse.caption());
		if (caption == null) {
			log.warn("처리 결과에 캡션이 없어 PROCESSING 상태를 유지합니다. jobId={}", jobId);
			return new LinkSyncOrchestrator.ProcessingSyncSnapshot(LinkAnalysisStatus.PROCESSING, null);
		}

		return new LinkSyncOrchestrator.ProcessingSyncSnapshot(LinkAnalysisStatus.SUCCEEDED, caption);
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}

