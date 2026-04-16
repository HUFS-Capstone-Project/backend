package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import java.time.Instant;

public record LinkStatusResult(
		Long linkId,
		String originalUrl,
		String processingJobId,
		LinkAnalysisStatus status,
		String captionRaw,
		Instant createdAt,
		Instant updatedAt
) {

	public static LinkStatusResult from(Link link) {
		return new LinkStatusResult(
				link.getId(),
				link.getOriginalUrl(),
				link.getProcessingJobId(),
				link.getStatus(),
				link.getCaptionRaw(),
				link.getCreatedAt(),
				link.getUpdatedAt()
		);
	}

	public boolean completed() {
		return status != null && status.isTerminal();
	}
}
