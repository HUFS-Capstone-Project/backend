package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import java.util.List;

public record LinkAnalysisResult(
		Long linkId,
		LinkAnalysisStatus status,
		String captionRaw,
		String extractionStoreName,
		String extractionAddress,
		String extractionCertainty,
		List<LinkPlaceResult> candidatePlaces,
		String errorCode,
		String errorMessage
) {

	public LinkAnalysisResult(
			Long linkId,
			LinkAnalysisStatus status,
			String captionRaw,
			String errorCode,
			String errorMessage
	) {
		this(linkId, status, captionRaw, null, null, null, List.of(), errorCode, errorMessage);
	}
}
