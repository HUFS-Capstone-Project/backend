package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.entity.RoomLinkCandidateOverride;

public record RoomLinkCandidateOverrideResult(
		Long candidateId,
		Long overrideId,
		String kakaoPlaceId,
		String name
) {

	public static RoomLinkCandidateOverrideResult from(RoomLinkCandidateOverride override) {
		return new RoomLinkCandidateOverrideResult(
				override.getLinkCandidate().getId(),
				override.getId(),
				override.getKakaoPlaceId(),
				override.getName()
		);
	}
}
