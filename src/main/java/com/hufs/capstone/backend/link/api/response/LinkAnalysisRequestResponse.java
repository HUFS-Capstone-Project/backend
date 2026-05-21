package com.hufs.capstone.backend.link.api.response;

import com.hufs.capstone.backend.link.application.dto.LinkAnalysisRequestResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;

public record LinkAnalysisRequestResponse(
		Long analysisRequestId,
		Long linkId,
		String jobId,
		LinkAnalysisStatus status,
		String errorCode,
		String errorMessage,
		Boolean retryable,
		Integer cooldownSeconds
) {

	public static LinkAnalysisRequestResponse from(LinkAnalysisRequestResult result) {
		return new LinkAnalysisRequestResponse(
				result.analysisRequestId(),
				result.linkId(),
				result.processingJobId(),
				result.status(),
				result.errorCode(),
				result.errorMessage(),
				result.retryable(),
				result.cooldownSeconds()
		);
	}
}
