package com.hufs.capstone.backend.link.api.response;

import com.hufs.capstone.backend.link.application.dto.RegisterLinkResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;

public record RegisterLinkResponse(
		Long linkId,
		String jobId,
		LinkAnalysisStatus status
) {

	public static RegisterLinkResponse from(RegisterLinkResult result) {
		return new RegisterLinkResponse(result.linkId(), result.processingJobId(), result.status());
	}
}
