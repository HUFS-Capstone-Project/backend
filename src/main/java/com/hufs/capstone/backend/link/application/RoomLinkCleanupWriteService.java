package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.domain.repository.LinkAnalysisRequestRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkCandidateOverrideRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceMemoRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceOriginRepository;
import com.hufs.capstone.backend.room.application.port.RoomDataCleanupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Order(100)
@RequiredArgsConstructor
public class RoomLinkCleanupWriteService implements RoomDataCleanupPort {

	private final LinkAnalysisRequestRepository linkAnalysisRequestRepository;
	private final RoomPlaceOriginRepository roomPlaceOriginRepository;
	private final RoomPlaceMemoRepository roomPlaceMemoRepository;
	private final RoomPlaceRepository roomPlaceRepository;
	private final RoomLinkCandidateOverrideRepository roomLinkCandidateOverrideRepository;
	private final RoomLinkRepository roomLinkRepository;

	@Transactional(propagation = Propagation.MANDATORY)
	@Override
	public void deleteAllByRoomId(Long roomId) {
		roomPlaceOriginRepository.deleteByRoomId(roomId);
		roomPlaceMemoRepository.deleteByRoomId(roomId);
		roomPlaceRepository.deleteByRoomId(roomId);
		linkAnalysisRequestRepository.deleteByRoomId(roomId);
		roomLinkCandidateOverrideRepository.deleteByRoomId(roomId);
		roomLinkRepository.deleteByRoomId(roomId);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void deleteAnalysisRequestsOnlyByRoomId(Long roomId) {
		linkAnalysisRequestRepository.deleteByRoomId(roomId);
	}
}
