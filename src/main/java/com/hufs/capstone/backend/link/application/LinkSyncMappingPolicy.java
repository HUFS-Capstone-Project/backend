package com.hufs.capstone.backend.link.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResultResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResponse;
import com.hufs.capstone.backend.link.application.dto.ProcessingResultSnapshot;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.vo.PlaceCandidateSnapshot;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LinkSyncMappingPolicy {

	private static final String MALFORMED_RESULT_ERROR_CODE = "PROCESSING_MALFORMED_RESPONSE";
	private static final String MALFORMED_RESULT_ERROR_MESSAGE = "Processing result response is malformed.";

	private final ObjectMapper objectMapper;
	private final LinkPlaceCandidateSnapshotMapper placeCandidateSnapshotMapper;

	public LinkSyncMappingPolicy(ObjectMapper objectMapper, LinkPlaceCandidateSnapshotMapper placeCandidateSnapshotMapper) {
		this.objectMapper = objectMapper;
		this.placeCandidateSnapshotMapper = placeCandidateSnapshotMapper;
	}

	public LinkSyncOrchestrator.ProcessingSyncSnapshot pendingSnapshot() {
		return new LinkSyncOrchestrator.ProcessingSyncSnapshot(LinkAnalysisStatus.REQUESTED, null, null, null);
	}

	public LinkSyncOrchestrator.ProcessingSyncSnapshot fromObservedStatus(
			LinkAnalysisStatus observedStatus,
			ProcessingJobResponse jobResponse
	) {
		return switch (observedStatus) {
			case FAILED -> new LinkSyncOrchestrator.ProcessingSyncSnapshot(
					LinkAnalysisStatus.FAILED,
					null,
					trimToNull(jobResponse.errorCode()),
					trimToNull(jobResponse.errorMessage())
			);
			case REQUESTED -> new LinkSyncOrchestrator.ProcessingSyncSnapshot(LinkAnalysisStatus.REQUESTED, null, null, null);
			case PROCESSING -> new LinkSyncOrchestrator.ProcessingSyncSnapshot(LinkAnalysisStatus.PROCESSING, null, null, null);
			case DISPATCH_FAILED -> new LinkSyncOrchestrator.ProcessingSyncSnapshot(
					LinkAnalysisStatus.DISPATCH_FAILED,
					null,
					trimToNull(jobResponse.errorCode()),
					trimToNull(jobResponse.errorMessage())
			);
			case SUCCEEDED -> throw new IllegalArgumentException("SUCCEEDED status requires result payload mapping.");
		};
	}

	public LinkSyncOrchestrator.ProcessingSyncSnapshot fromSucceededResult(
			String jobId,
			ProcessingJobResultResponse resultResponse
	) {
		if (resultResponse == null) {
			return new LinkSyncOrchestrator.ProcessingSyncSnapshot(LinkAnalysisStatus.PROCESSING, null, null, null);
		}

		try {
			return new LinkSyncOrchestrator.ProcessingSyncSnapshot(
					LinkAnalysisStatus.SUCCEEDED,
					toResultSnapshot(resultResponse),
					null,
					null
			);
		} catch (IllegalArgumentException ex) {
			log.warn("Processing result response is malformed. jobId={}", jobId, ex);
			return new LinkSyncOrchestrator.ProcessingSyncSnapshot(
					LinkAnalysisStatus.FAILED,
					null,
					MALFORMED_RESULT_ERROR_CODE,
					MALFORMED_RESULT_ERROR_MESSAGE
			);
		}
	}

	private ProcessingResultSnapshot toResultSnapshot(ProcessingJobResultResponse response) {
		ProcessingJobResultResponse.ExtractionResultResponse extraction = response.extractionResult();
		List<PlaceCandidateSnapshot> extractedPlaces =
				placeCandidateSnapshotMapper.fromProcessingCandidates(response.selectedPlaces());
		return new ProcessingResultSnapshot(
				trimToNull(response.caption()),
				extraction == null ? null : trimToNull(extraction.storeName()),
				extraction == null ? null : trimToNull(extraction.address()),
				extraction == null ? null : trimToNull(extraction.certainty()),
				placeCandidateSnapshotMapper.write(extractedPlaces),
				writeJson(response)
		);
	}

	private String writeJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Cannot serialize processing result.", ex);
		}
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
