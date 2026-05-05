package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkAnalysisRequest;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.List;
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
	private final LinkAnalysisResultMapper linkAnalysisResultMapper;
	private final RoomPlaceRepository roomPlaceRepository;

	public LinkAnalysisResult getLinkAnalysisResult(Long userId, String roomId, Long analysisRequestId) {
		LinkAnalysisRequest analysisRequest =
				linkAnalysisAuthorizationService.requireAnalysisRequest(userId, roomId, analysisRequestId);
		Room room = analysisRequest.getRoom();
		Long linkId = analysisRequest.getLink().getId();
		LinkAnalysisResult result = linkAnalysisCacheCoordinator.getOrLoad(linkId, () -> resolveCurrentStatus(linkId));
		List<String> kakaoPlaceIds = result.candidatePlaces().stream()
				.map(candidate -> candidate.kakaoPlaceId())
				.filter(kakaoPlaceId -> kakaoPlaceId != null && !kakaoPlaceId.isBlank())
				.toList();
		List<RoomPlace> savedPlaces = kakaoPlaceIds.isEmpty()
				? List.of()
				: roomPlaceRepository.findExistingByRoomIdAndKakaoPlaceIds(room.getId(), kakaoPlaceIds);
		return linkAnalysisResultMapper.withSavedStatus(result, savedPlaces);
	}

	private LinkAnalysisResult resolveCurrentStatus(Long linkId) {
		Link snapshot = linkRepository.findById(linkId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "링크를 찾을 수 없습니다."));

		LinkSyncOrchestrator.ProcessingSyncSnapshot syncSnapshot =
				snapshot.isTerminal() ? null : linkSyncOrchestrator.resolve(snapshot);

		LinkAnalysisStatusResolver.Resolution resolution = linkAnalysisStatusResolver.resolve(snapshot, syncSnapshot);
		if (!resolution.requiresWrite()) {
			return linkAnalysisResultMapper.from(snapshot);
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
