package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResultResponse;
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
}
