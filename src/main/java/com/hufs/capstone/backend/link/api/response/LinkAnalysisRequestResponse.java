package com.hufs.capstone.backend.link.api.response;

import com.hufs.capstone.backend.link.application.dto.LinkAnalysisRequestResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;

public record LinkAnalysisRequestResponse(
		Long linkId,
		String jobId,
		LinkAnalysisStatus status
) {

	public static LinkAnalysisRequestResponse from(LinkAnalysisRequestResult result) {
		return new LinkAnalysisRequestResponse(result.linkId(), result.processingJobId(), result.status());
	}
}
