package com.hufs.capstone.backend.link.application;

import static org.mockito.Mockito.verify;

import com.hufs.capstone.backend.link.domain.repository.LinkAnalysisRequestRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomPlaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomLinkCleanupWriteServiceTest {

	@Mock
	private LinkAnalysisRequestRepository linkAnalysisRequestRepository;

	@Mock
	private RoomPlaceRepository roomPlaceRepository;

	@Mock
	private RoomLinkRepository roomLinkRepository;

	@InjectMocks
	private RoomLinkCleanupWriteService roomLinkCleanupWriteService;

	@Test
	void deleteAllByRoomIdShouldCallRepositoryDelete() {
		roomLinkCleanupWriteService.deleteAllByRoomId(1L);

		verify(roomPlaceRepository).deleteByRoomId(1L);
		verify(linkAnalysisRequestRepository).deleteByRoomId(1L);
		verify(roomLinkRepository).deleteByRoomId(1L);
	}
}
