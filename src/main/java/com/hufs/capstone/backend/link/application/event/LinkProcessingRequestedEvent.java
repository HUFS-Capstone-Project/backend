package com.hufs.capstone.backend.link.application.event;

public record LinkProcessingRequestedEvent(
		Long linkId,
		String normalizedUrl,
		String roomId,
		String source
) {
}
