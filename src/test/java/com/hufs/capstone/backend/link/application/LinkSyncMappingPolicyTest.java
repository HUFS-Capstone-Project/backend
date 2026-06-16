package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.external.processing.ProcessingErrorCodes;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResultResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResponse;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.LinkSourceType;
import java.util.List;
import org.junit.jupiter.api.Test;

class LinkSyncMappingPolicyTest {

	private final LinkSyncMappingPolicy policy = new LinkSyncMappingPolicy(
			new ObjectMapper(),
			new LinkPlaceCandidateSnapshotMapper(new ObjectMapper())
	);

	@Test
	void shouldMapValidProcessingSourceTypeToLinkSourceType() {
		LinkSyncOrchestrator.ProcessingSyncSnapshot snapshot = policy.fromSucceededResult(
				"job-1",
				result("YOUTUBE")
		);

		assertThat(snapshot.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(snapshot.result().linkSourceType()).isEqualTo(LinkSourceType.YOUTUBE);
	}

	@Test
	void shouldMapUnknownProcessingSourceTypeToNullSoExistingLinkSourceTypeIsKept() {
		LinkSyncOrchestrator.ProcessingSyncSnapshot snapshot = policy.fromSucceededResult(
				"job-1",
				result("UNKNOWN")
		);

		assertThat(snapshot.status()).isEqualTo(LinkAnalysisStatus.SUCCEEDED);
		assertThat(snapshot.result().linkSourceType()).isNull();
	}

	@Test
	void shouldMapUnsupportedPlatformUrlResultToUserMessageAndNonRetryableFailure() {
		LinkSyncOrchestrator.ProcessingSyncSnapshot snapshot = policy.fromSucceededResult(
				"job-1",
				failedResult(ProcessingErrorCodes.UNSUPPORTED_PLATFORM_URL)
		);

		assertThat(snapshot.status()).isEqualTo(LinkAnalysisStatus.FAILED);
		assertThat(snapshot.errorCode()).isEqualTo(ProcessingErrorCodes.UNSUPPORTED_PLATFORM_URL);
		assertThat(snapshot.errorMessage()).isEqualTo("지원하지 않는 링크 형식입니다.");
		assertThat(snapshot.retryable()).isFalse();
	}

	@Test
	void shouldMapUnsupportedPlatformUrlObservedStatusToUserMessageAndNonRetryableFailure() {
		LinkSyncOrchestrator.ProcessingSyncSnapshot snapshot = policy.fromObservedStatus(
				LinkAnalysisStatus.FAILED,
				new ProcessingJobResponse(
						"job-1",
						"FAILED",
						"https://www.youtube.com/channel/abc",
						"room-1",
						null,
						ProcessingErrorCodes.UNSUPPORTED_PLATFORM_URL,
						"UnsupportedPlatformUrlError: Unsupported or malformed youtube URL"
				)
		);

		assertThat(snapshot.status()).isEqualTo(LinkAnalysisStatus.FAILED);
		assertThat(snapshot.errorCode()).isEqualTo(ProcessingErrorCodes.UNSUPPORTED_PLATFORM_URL);
		assertThat(snapshot.errorMessage()).isEqualTo("지원하지 않는 링크 형식입니다.");
		assertThat(snapshot.retryable()).isFalse();
	}

	@Test
	void shouldPreserveInstagramRateLimitCooldownFromResult() {
		LinkSyncOrchestrator.ProcessingSyncSnapshot snapshot = policy.fromSucceededResult(
				"job-1",
				new ProcessingJobResultResponse(
						"job-1",
						"FAILED",
						"https://www.instagram.com/reel/abc/",
						"https://www.instagram.com/reel/abc/",
						"https://www.instagram.com/reel/abc/",
						null,
						null,
						List.of(),
						ProcessingErrorCodes.INSTAGRAM_RATE_LIMITED,
						"Instagram crawl returned HTTP 429.",
						true,
						1800
				)
		);

		assertThat(snapshot.status()).isEqualTo(LinkAnalysisStatus.FAILED);
		assertThat(snapshot.errorCode()).isEqualTo(ProcessingErrorCodes.INSTAGRAM_RATE_LIMITED);
		assertThat(snapshot.retryable()).isTrue();
		assertThat(snapshot.cooldownSeconds()).isEqualTo(1800);
	}

	private static ProcessingJobResultResponse result(String sourceType) {
		return new ProcessingJobResultResponse(
				"job-1",
				"SUCCEEDED",
				"https://example.com/post/1",
				null,
				null,
				new ProcessingJobResultResponse.ContentResponse(
						sourceType,
						"content",
						null,
						null,
						null,
						List.of(),
						null
				),
				null,
				List.of(),
				null,
				null,
				null
		);
	}

	private static ProcessingJobResultResponse failedResult(String errorCode) {
		return new ProcessingJobResultResponse(
				"job-1",
				"FAILED",
				"https://www.youtube.com/channel/abc",
				null,
				null,
				null,
				null,
				List.of(),
				errorCode,
				"UnsupportedPlatformUrlError: Unsupported or malformed youtube URL",
				false
		);
	}
}
