package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkAnalysisRequestRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomLinkCleanupWriteService {

	private final LinkAnalysisRequestRepository linkAnalysisRequestRepository;
	private final RoomPlaceRepository roomPlaceRepository;
	private final RoomLinkRepository roomLinkRepository;

	@Transactional(propagation = Propagation.MANDATORY)
	public void deleteAllByRoomId(Long roomId) {
		roomPlaceRepository.deleteByRoomId(roomId);
		linkAnalysisRequestRepository.deleteByRoomId(roomId);
		roomLinkRepository.deleteByRoomId(roomId);
	}
}
