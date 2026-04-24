package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;

public record LinkAnalysisRequestResult(
		Long linkId,
		String processingJobId,
		LinkAnalysisStatus status,
		boolean createdRequest
) {

	public static LinkAnalysisRequestResult from(Link link, boolean createdRequest) {
		return new LinkAnalysisRequestResult(
				link.getId(),
				link.getProcessingJobId(),
				link.getStatus(),
				createdRequest
		);
	}
}
