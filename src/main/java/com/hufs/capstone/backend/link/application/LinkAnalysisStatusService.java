package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkAnalysisRequest;
import com.hufs.capstone.backend.link.domain.entity.LinkCandidate;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.link.domain.entity.RoomLinkCandidateOverride;
import com.hufs.capstone.backend.link.domain.repository.LinkCandidateRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkCandidateOverrideRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkAnalysisStatusService {

	private final LinkAnalysisAuthorizationService linkAnalysisAuthorizationService;
	private final LinkAnalysisCacheCoordinator linkAnalysisCacheCoordinator;
	private final LinkRepository linkRepository;
	private final LinkSyncOrchestrator linkSyncOrchestrator;
	private final LinkAnalysisStatusResolver linkAnalysisStatusResolver;
	private final LinkAnalysisStatusWriteService linkAnalysisStatusWriteService;
	private final LinkAnalysisResultAssembler linkAnalysisResultAssembler;
	private final RoomPlaceRepository roomPlaceRepository;
	private final LinkCandidateRepository linkCandidateRepository;
	private final RoomLinkRepository roomLinkRepository;
	private final RoomLinkCandidateOverrideRepository overrideRepository;

	public LinkAnalysisResult getLinkAnalysisResult(Long userId, String roomId, Long analysisRequestId) {
		LinkAnalysisRequest analysisRequest =
				linkAnalysisAuthorizationService.requireAnalysisRequest(userId, roomId, analysisRequestId);
		Room room = analysisRequest.getRoom();
		Long linkId = analysisRequest.getLink().getId();
		LinkAnalysisResult result = linkAnalysisCacheCoordinator.getOrLoad(linkId, () -> resolveCurrentStatus(linkId));
		List<LinkCandidate> originalCandidates = linkCandidateRepository.findByLinkIdOrderByCandidateOrderAscIdAsc(linkId);
		if (!originalCandidates.isEmpty()) {
			return applyRoomCandidateContext(room, linkId, result, originalCandidates);
		}
		List<String> kakaoPlaceIds = result.candidatePlaces().stream()
				.map(candidate -> candidate.kakaoPlaceId())
				.filter(kakaoPlaceId -> kakaoPlaceId != null && !kakaoPlaceId.isBlank())
				.toList();
		List<RoomPlace> savedPlaces = kakaoPlaceIds.isEmpty()
				? List.of()
				: roomPlaceRepository.findExistingByRoomIdAndKakaoPlaceIds(room.getId(), kakaoPlaceIds);
		return linkAnalysisResultAssembler.withSavedStatus(result, savedPlaces);
	}

	private LinkAnalysisResult applyRoomCandidateContext(
			Room room,
			Long linkId,
			LinkAnalysisResult result,
			List<LinkCandidate> originalCandidates
	) {
		Optional<RoomLink> roomLink = roomLinkRepository.findByRoomAndLinkId(room, linkId);
		List<RoomLinkCandidateOverride> overrides = roomLink
				.map(value -> overrideRepository.findByRoomLinkId(value.getId()))
				.orElseGet(List::of);
		List<String> effectiveKakaoPlaceIds = originalCandidates.stream()
				.map(candidate -> effectiveKakaoPlaceId(candidate, overrides))
				.filter(kakaoPlaceId -> kakaoPlaceId != null && !kakaoPlaceId.isBlank())
				.toList();
		List<RoomPlace> savedPlaces = effectiveKakaoPlaceIds.isEmpty()
				? List.of()
				: roomPlaceRepository.findExistingByRoomIdAndKakaoPlaceIds(room.getId(), effectiveKakaoPlaceIds);
		return linkAnalysisResultAssembler.withRoomCandidateContext(result, originalCandidates, overrides, savedPlaces);
	}

	private static String effectiveKakaoPlaceId(LinkCandidate candidate, List<RoomLinkCandidateOverride> overrides) {
		return overrides.stream()
				.filter(override -> candidate.getId().equals(override.getLinkCandidate().getId()))
				.findFirst()
				.map(RoomLinkCandidateOverride::getKakaoPlaceId)
				.orElse(candidate.getKakaoPlaceId());
	}

	private LinkAnalysisResult resolveCurrentStatus(Long linkId) {
		Link snapshot = linkRepository.findById(linkId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "링크를 찾을 수 없습니다."));

		LinkSyncOrchestrator.ProcessingSyncSnapshot syncSnapshot =
				snapshot.isTerminal() ? null : linkSyncOrchestrator.resolve(snapshot);

		LinkAnalysisStatusResolver.Resolution resolution = linkAnalysisStatusResolver.resolve(snapshot, syncSnapshot);
		if (!resolution.requiresWrite()) {
			return linkAnalysisResultAssembler.from(snapshot);
		}
		return linkAnalysisStatusWriteService.applySyncSnapshot(
				linkId,
				resolution.targetStatus(),
				resolution.result(),
				resolution.errorCode(),
				resolution.errorMessage()
		);
	}
}
