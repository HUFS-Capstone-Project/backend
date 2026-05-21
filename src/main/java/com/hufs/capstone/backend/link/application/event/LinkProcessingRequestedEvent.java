package com.hufs.capstone.backend.link.application.event;

public record LinkProcessingRequestedEvent(
		Long linkId,
		String originalUrl,
		String canonicalUrl,
		String roomId,
		String source
) {
}
