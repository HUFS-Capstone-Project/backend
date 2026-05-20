package com.hufs.capstone.backend.link.application.dto;

public record ProcessingResultSnapshot(
		String sourceUrl,
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
				null
		);
	}
}
