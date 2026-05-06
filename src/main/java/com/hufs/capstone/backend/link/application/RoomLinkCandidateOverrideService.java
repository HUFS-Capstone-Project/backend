package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.RoomLinkCandidateOverrideResult;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkAnalysisRequest;
import com.hufs.capstone.backend.link.domain.entity.LinkCandidate;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.link.domain.entity.RoomLinkCandidateOverride;
import com.hufs.capstone.backend.link.domain.repository.LinkCandidateRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkCandidateOverrideRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import com.hufs.capstone.backend.room.domain.entity.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomLinkCandidateOverrideService {

	private final LinkAnalysisAuthorizationService linkAnalysisAuthorizationService;
	private final LinkCandidateRepository linkCandidateRepository;
	private final RoomLinkRepository roomLinkRepository;
	private final RoomLinkCandidateOverrideRepository overrideRepository;

	@Transactional
	public RoomLinkCandidateOverrideResult overrideCandidate(
			Long userId,
			String roomId,
			Long analysisRequestId,
			Long candidateId,
			PlaceSnapshot snapshot
	) {
		if (snapshot == null || !snapshot.hasKakaoPlaceId()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "Valid Kakao place snapshot is required.");
		}
		LinkAnalysisRequest analysisRequest =
				linkAnalysisAuthorizationService.requireAnalysisRequestForUpdate(userId, roomId, analysisRequestId);
		Link link = analysisRequest.getLink();
		LinkCandidate candidate = linkCandidateRepository.findByIdAndLinkId(candidateId, link.getId())
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "Link candidate not found."));
		RoomLink roomLink = findOrCreateRoomLink(analysisRequest.getRoom(), link);
		RoomLinkCandidateOverride override = overrideRepository
				.findByRoomLinkIdAndLinkCandidateIdForUpdate(roomLink.getId(), candidate.getId())
				.orElse(null);
		if (override == null) {
			override = createOverrideWithDuplicateRetry(roomLink, candidate, userId, snapshot);
		} else {
			override.update(userId, snapshot);
		}
		return RoomLinkCandidateOverrideResult.from(override);
	}

	private RoomLink findOrCreateRoomLink(Room room, Link link) {
		RoomLink existing = roomLinkRepository.findByRoomAndLinkId(room, link.getId()).orElse(null);
		if (existing != null) {
			return existing;
		}
		try {
			return roomLinkRepository.saveAndFlush(RoomLink.bind(room, link));
		} catch (DataIntegrityViolationException ex) {
			return roomLinkRepository.findByRoomAndLinkId(room, link.getId())
					.orElseThrow(() -> new BusinessException(
							ErrorCode.E409_CONFLICT,
							"Room link creation conflict occurred.",
							ex
					));
		}
	}

	private RoomLinkCandidateOverride createOverrideWithDuplicateRetry(
			RoomLink roomLink,
			LinkCandidate candidate,
			Long userId,
			PlaceSnapshot snapshot
	) {
		try {
			return overrideRepository.save(RoomLinkCandidateOverride.create(roomLink, candidate, userId, snapshot));
		} catch (DataIntegrityViolationException ex) {
			RoomLinkCandidateOverride existing = overrideRepository
					.findByRoomLinkIdAndLinkCandidateIdForUpdate(roomLink.getId(), candidate.getId())
					.orElseThrow(() -> new BusinessException(
							ErrorCode.E409_CONFLICT,
							"Candidate override conflict occurred.",
							ex
					));
			existing.update(userId, snapshot);
			return existing;
		}
	}
}
