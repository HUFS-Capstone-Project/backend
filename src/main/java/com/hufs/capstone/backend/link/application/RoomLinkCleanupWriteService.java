package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.domain.repository.LinkAnalysisRequestRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceMemoRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomLinkCleanupWriteService {

	private final LinkAnalysisRequestRepository linkAnalysisRequestRepository;
	private final RoomPlaceSourceRepository roomPlaceSourceRepository;
	private final RoomPlaceMemoRepository roomPlaceMemoRepository;
	private final RoomPlaceRepository roomPlaceRepository;
	private final RoomLinkRepository roomLinkRepository;

	@Transactional(propagation = Propagation.MANDATORY)
	public void deleteAllByRoomId(Long roomId) {
		roomPlaceSourceRepository.deleteByRoomId(roomId);
		roomPlaceMemoRepository.deleteByRoomId(roomId);
		roomPlaceRepository.deleteByRoomId(roomId);
		linkAnalysisRequestRepository.deleteByRoomId(roomId);
		roomLinkRepository.deleteByRoomId(roomId);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void deleteAnalysisRequestsOnlyByRoomId(Long roomId) {
		linkAnalysisRequestRepository.deleteByRoomId(roomId);
	}
}
