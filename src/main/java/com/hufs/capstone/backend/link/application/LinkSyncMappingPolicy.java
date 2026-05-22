package com.hufs.capstone.backend.link.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.external.processing.ProcessingErrorCodes;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResultResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResultResponse.ResolvedPlaceResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResponse;
import com.hufs.capstone.backend.link.application.dto.ProcessingResultSnapshot;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.LinkSourceTypeResolver;
import com.hufs.capstone.backend.link.domain.vo.PlaceCandidateSnapshot;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LinkSyncMappingPolicy {

	private static final String MALFORMED_RESULT_ERROR_CODE = "PROCESSING_MALFORMED_RESPONSE";
	private static final String MALFORMED_RESULT_ERROR_MESSAGE = "Processing result response is malformed.";
	private static final String UNSUPPORTED_PLATFORM_URL_USER_MESSAGE = "지원하지 않는 링크 형식입니다.";
	private static final String INSTAGRAM_RATE_LIMITED_USER_MESSAGE =
			"현재 Instagram 분석이 일시적으로 제한되어 있어요. 잠시 후 다시 시도해 주세요.";

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
			case FAILED -> failedSnapshot(trimToNull(jobResponse.errorCode()), trimToNull(jobResponse.errorMessage()), null);
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
		if (isInstagramRateLimitedFailure(resultResponse)) {
			return new LinkSyncOrchestrator.ProcessingSyncSnapshot(
					LinkAnalysisStatus.FAILED,
					null,
					ProcessingErrorCodes.INSTAGRAM_RATE_LIMITED,
					INSTAGRAM_RATE_LIMITED_USER_MESSAGE,
					Boolean.TRUE.equals(resultResponse.retryable()),
					null
			);
		}
		if (isFailed(resultResponse)) {
			return failedSnapshot(
					trimToNull(resultResponse.errorCode()),
					trimToNull(resultResponse.errorMessage()),
					resultResponse.retryable()
			);
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

	private static boolean isInstagramRateLimitedFailure(ProcessingJobResultResponse response) {
		return isFailed(response)
				&& ProcessingErrorCodes.INSTAGRAM_RATE_LIMITED.equals(trimToNull(response.errorCode()));
	}

	private static boolean isFailed(ProcessingJobResultResponse response) {
		return LinkAnalysisStatus.fromProcessingStatus(response.status()) == LinkAnalysisStatus.FAILED;
	}

	private static LinkSyncOrchestrator.ProcessingSyncSnapshot failedSnapshot(
			String errorCode,
			String errorMessage,
			Boolean retryable
	) {
		if (ProcessingErrorCodes.UNSUPPORTED_PLATFORM_URL.equals(errorCode)) {
			return new LinkSyncOrchestrator.ProcessingSyncSnapshot(
					LinkAnalysisStatus.FAILED,
					null,
					ProcessingErrorCodes.UNSUPPORTED_PLATFORM_URL,
					UNSUPPORTED_PLATFORM_URL_USER_MESSAGE,
					false,
					null
			);
		}
		return new LinkSyncOrchestrator.ProcessingSyncSnapshot(
				LinkAnalysisStatus.FAILED,
				null,
				errorCode,
				errorMessage,
				retryable,
				null
		);
	}

	private ProcessingResultSnapshot toResultSnapshot(ProcessingJobResultResponse response) {
		ResolvedPlaceResponse representativePlace = representativePlace(response);
		ProcessingJobResultResponse.LinkStatsResponse linkStats = response.linkStats();
		List<PlaceCandidateSnapshot> extractedPlaces =
				placeCandidateSnapshotMapper.fromProcessingCandidates(response.resolvedPlaces());
		return new ProcessingResultSnapshot(
				trimToNull(response.originalUrl()),
				LinkSourceTypeResolver.fromProcessingSourceType(response.content() == null ? null : response.content().sourceType()),
				trimToNull(response.content() == null ? null : response.content().contentText()),
				linkStats == null ? null : linkStats.likeCount(),
				linkStats == null ? null : linkStats.commentCount(),
				linkStats == null ? null : trimToNull(linkStats.postedAt()),
				representativePlace == null ? null : trimToNull(representativePlace.placeName()),
				representativePlace == null ? null : representativeAddress(representativePlace),
				null,
				placeCandidateSnapshotMapper.write(extractedPlaces),
				writeJson(response)
		);
	}

	private static ResolvedPlaceResponse representativePlace(ProcessingJobResultResponse response) {
		List<ResolvedPlaceResponse> resolvedPlaces = response.resolvedPlaces();
		return resolvedPlaces == null || resolvedPlaces.isEmpty() ? null : resolvedPlaces.get(0);
	}

	private static String representativeAddress(ResolvedPlaceResponse place) {
		String address = trimToNull(place.address());
		return address == null ? trimToNull(place.roadAddress()) : address;
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
