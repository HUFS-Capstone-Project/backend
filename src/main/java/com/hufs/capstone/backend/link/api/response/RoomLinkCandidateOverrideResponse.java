package com.hufs.capstone.backend.link.api.response;

import com.hufs.capstone.backend.link.application.dto.RoomLinkCandidateOverrideResult;

public record RoomLinkCandidateOverrideResponse(
		Long candidateId,
		Long overrideId,
		String kakaoPlaceId,
		String name
) {

	public static RoomLinkCandidateOverrideResponse from(RoomLinkCandidateOverrideResult result) {
		return new RoomLinkCandidateOverrideResponse(
				result.candidateId(),
				result.overrideId(),
				result.kakaoPlaceId(),
				result.name()
		);
	}
}
