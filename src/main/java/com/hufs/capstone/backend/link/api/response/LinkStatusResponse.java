package com.hufs.capstone.backend.link.api.response;

import com.hufs.capstone.backend.link.application.dto.LinkStatusResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import java.time.Instant;

public record LinkStatusResponse(
		Long linkId,
		String originalUrl,
		String jobId,
		LinkAnalysisStatus status,
		boolean completed,
		String caption,
		Instant createdAt,
		Instant updatedAt
) {

	public static LinkStatusResponse from(LinkStatusResult result) {
		return new LinkStatusResponse(
				result.linkId(),
				result.originalUrl(),
				result.processingJobId(),
				result.status(),
				result.completed(),
				result.captionRaw(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
