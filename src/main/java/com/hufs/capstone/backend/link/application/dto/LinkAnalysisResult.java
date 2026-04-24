package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;

public record LinkAnalysisResult(
		Long linkId,
		LinkAnalysisStatus status,
		String captionRaw,
		String errorCode,
		String errorMessage
) {

	public static LinkAnalysisResult from(Link link) {
		return new LinkAnalysisResult(
				link.getId(),
				link.getStatus(),
				link.getCaptionRaw(),
				link.getErrorCode(),
				link.getErrorMessage()
		);
	}
}
