package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
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

	public void assertReadable(Long userId, String roomId, Long linkId) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		boolean requestedInRoom = linkAnalysisRequestRepository.existsByRoomAndLinkId(room, linkId);
		if (!requestedInRoom) {
			throw new BusinessException(ErrorCode.E403_FORBIDDEN, "해당 방에서 요청한 링크 분석 이력이 없습니다.");
		}
	}
}
