package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.LinkSourceType;
import java.util.List;

public record LinkAnalysisResult(
		Long linkId,
		LinkAnalysisStatus status,
		String originalUrl,
		LinkSourceType linkSourceType,
		String contentText,
		LinkStatsResult linkStats,
		String extractionStoreName,
		String extractionAddress,
		String extractionCertainty,
		List<LinkPlaceResult> candidatePlaces,
		String errorCode,
		String errorMessage,
		Boolean retryable,
		Integer cooldownSeconds
) {

	public LinkAnalysisResult(
			Long linkId,
			LinkAnalysisStatus status,
			String contentText,
			String errorCode,
			String errorMessage
	) {
		this(
				linkId,
				status,
				null,
				null,
				contentText,
				new LinkStatsResult(null, null, null),
				null,
				null,
				null,
				List.of(),
				errorCode,
				errorMessage,
				null,
				null
		);
	}
}
