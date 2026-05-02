package com.hufs.capstone.backend.link.application.dto;

public record ProcessingResultSnapshot(
		String captionRaw,
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
				null
		);
	}
}
