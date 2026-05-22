package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.LinkSourceType;

public record ProcessingResultSnapshot(
		String originalUrl,
		LinkSourceType linkSourceType,
		String contentText,
		Long likeCount,
		Long commentCount,
		String postedAt,
		String extractionStoreName,
		String extractionAddress,
		String extractionCertainty,
		String extractedPlacesJson,
		String processingResultJson
) {

	public static ProcessingResultSnapshot empty() {
		return new ProcessingResultSnapshot(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);
	}
}
