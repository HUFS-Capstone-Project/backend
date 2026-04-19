package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;

public record RegisterLinkResult(
		Long linkId,
		String processingJobId,
		LinkAnalysisStatus status,
		ProcessingDispatchStatus dispatchStatus
) {

	public static RegisterLinkResult from(Link link) {
		return new RegisterLinkResult(
				link.getId(),
				link.getProcessingJobId(),
				link.getStatus(),
				link.getDispatchStatus()
		);
	}
}
