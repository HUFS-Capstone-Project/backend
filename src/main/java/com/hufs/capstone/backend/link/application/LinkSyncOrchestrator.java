package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.external.processing.ProcessingClient;
import com.hufs.capstone.backend.external.processing.ProcessingClientException;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResultResponse;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkSyncOrchestrator {

	private final ProcessingClient processingClient;
	private final LinkSyncMappingPolicy linkSyncMappingPolicy;
	private final LinkSyncFailurePolicy linkSyncFailurePolicy;

	public ProcessingSyncSnapshot resolve(Link link) {
		if (!link.isDispatchReady()) {
			return linkSyncMappingPolicy.pendingSnapshot();
		}
		String realJobId = link.getProcessingJobId();

		ProcessingJobResponse jobResponse = processingClient.getJob(realJobId);
		LinkAnalysisStatus observedStatus = LinkAnalysisStatus.fromProcessingStatus(jobResponse.status());
		if (observedStatus != LinkAnalysisStatus.SUCCEEDED) {
			return linkSyncMappingPolicy.fromObservedStatus(observedStatus);
		}

		ProcessingJobResultResponse resultResponse = getJobResultOrNull(realJobId);
		return linkSyncMappingPolicy.fromSucceededResult(realJobId, resultResponse);
	}

	private ProcessingJobResultResponse getJobResultOrNull(String realJobId) {
		try {
			return processingClient.getJobResult(realJobId);
		} catch (ProcessingClientException exception) {
			if (linkSyncFailurePolicy.isResultNotReady(exception)) {
				log.debug("처리 결과가 아직 준비되지 않았습니다. jobId={}", realJobId);
				return null;
			}
			throw exception;
		}
	}

	public record ProcessingSyncSnapshot(LinkAnalysisStatus status, String captionRaw) {
	}
}

