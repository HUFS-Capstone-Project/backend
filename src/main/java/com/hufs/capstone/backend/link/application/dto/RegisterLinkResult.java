package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;

public record RegisterLinkResult(
		Long linkId,
		String processingJobId,
		LinkAnalysisStatus status
) {
}
