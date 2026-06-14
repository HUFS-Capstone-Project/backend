package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.domain.entity.LinkAnalysisRequest;
import com.hufs.capstone.backend.link.domain.repository.LinkAnalysisRequestRepository;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkAnalysisAuthorizationService {

	private final RoomAccessService roomAccessService;
	private final LinkAnalysisRequestRepository linkAnalysisRequestRepository;

	public LinkAnalysisRequest requireAnalysisRequest(Long userId, String roomId, Long analysisRequestId) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		LinkAnalysisRequest analysisRequest = linkAnalysisRequestRepository.findWithRoomAndLinkById(analysisRequestId)
				.orElseThrow(() -> new BusinessException(ErrorCode.LINK_ANALYSIS_REQUEST_NOT_FOUND));
		requireSameRoom(room, analysisRequest);
		return analysisRequest;
	}

	public LinkAnalysisRequest requireAnalysisRequestForUpdate(Long userId, String roomId, Long analysisRequestId) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		LinkAnalysisRequest analysisRequest = linkAnalysisRequestRepository.findWithRoomAndLinkByIdForUpdate(analysisRequestId)
				.orElseThrow(() -> new BusinessException(ErrorCode.LINK_ANALYSIS_REQUEST_NOT_FOUND));
		requireSameRoom(room, analysisRequest);
		return analysisRequest;
	}

	private static void requireSameRoom(Room room, LinkAnalysisRequest analysisRequest) {
		if (!room.getId().equals(analysisRequest.getRoom().getId())) {
			throw new BusinessException(ErrorCode.LINK_ANALYSIS_REQUEST_FORBIDDEN);
		}
	}
}
