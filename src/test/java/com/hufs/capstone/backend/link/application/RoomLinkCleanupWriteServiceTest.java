package com.hufs.capstone.backend.link.application;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import com.hufs.capstone.backend.link.domain.repository.LinkAnalysisRequestRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkCandidateOverrideRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceMemoRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceOriginRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomLinkCleanupWriteServiceTest {

	@Mock
	private LinkAnalysisRequestRepository linkAnalysisRequestRepository;

	@Mock
	private RoomPlaceOriginRepository roomPlaceOriginRepository;

	@Mock
	private RoomPlaceMemoRepository roomPlaceMemoRepository;

	@Mock
	private RoomPlaceRepository roomPlaceRepository;

	@Mock
	private RoomLinkCandidateOverrideRepository roomLinkCandidateOverrideRepository;

	@Mock
	private RoomLinkRepository roomLinkRepository;

	@InjectMocks
	private RoomLinkCleanupWriteService roomLinkCleanupWriteService;

	@Test
	void deleteAllByRoomIdShouldCallRepositoryDelete() {
		roomLinkCleanupWriteService.deleteAllByRoomId(1L);

		verify(roomPlaceOriginRepository).deleteByRoomId(1L);
		verify(roomPlaceMemoRepository).deleteByRoomId(1L);
		verify(roomPlaceRepository).deleteByRoomId(1L);
		verify(linkAnalysisRequestRepository).deleteByRoomId(1L);
		InOrder inOrder = inOrder(roomLinkCandidateOverrideRepository, roomLinkRepository);
		inOrder.verify(roomLinkCandidateOverrideRepository).deleteByRoomId(1L);
		inOrder.verify(roomLinkRepository).deleteByRoomId(1L);
	}

	@Test
	void deleteAnalysisRequestsOnlyByRoomIdShouldNotDeleteSavedRoomData() {
		roomLinkCleanupWriteService.deleteAnalysisRequestsOnlyByRoomId(1L);

		verify(linkAnalysisRequestRepository).deleteByRoomId(1L);
	}
}
