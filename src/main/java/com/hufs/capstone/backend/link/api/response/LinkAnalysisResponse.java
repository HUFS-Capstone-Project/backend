package com.hufs.capstone.backend.link.api.response;

import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;

// TODO: processing 서버가 장소 후보를 제공하면 placeCandidates 응답 DTO를 별도로 확장한다.
public record LinkAnalysisResponse(
		Long linkId,
		LinkAnalysisStatus status,
		String caption,
		String errorCode,
		String errorMessage
) {

	public static LinkAnalysisResponse from(LinkAnalysisResult result) {
		return new LinkAnalysisResponse(
				result.linkId(),
				result.status(),
				result.captionRaw(),
				result.errorCode(),
				result.errorMessage()
		);
	}
}
