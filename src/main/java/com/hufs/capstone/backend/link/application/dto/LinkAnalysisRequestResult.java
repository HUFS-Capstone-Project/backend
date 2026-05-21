package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;

public record LinkAnalysisRequestResult(
		Long analysisRequestId,
		Long linkId,
		String processingJobId,
		LinkAnalysisStatus status,
		String errorCode,
		String errorMessage,
		Boolean retryable,
		Integer cooldownSeconds,
		boolean createdRequest
) {

	public LinkAnalysisRequestResult(
			Long linkId,
			String processingJobId,
			LinkAnalysisStatus status,
			boolean createdRequest
	) {
		this(null, linkId, processingJobId, status, null, null, null, null, createdRequest);
	}

	public static LinkAnalysisRequestResult from(Link link, Long analysisRequestId, boolean createdRequest) {
		return new LinkAnalysisRequestResult(
				analysisRequestId,
				link.getId(),
				link.getProcessingJobId(),
				link.getStatus(),
				link.getErrorCode(),
				link.getErrorMessage(),
				link.getRetryable(),
				link.getCooldownSeconds(),
				createdRequest
		);
	}
}
