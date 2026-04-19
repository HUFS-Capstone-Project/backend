package com.hufs.capstone.backend.link.application.event;

import static org.mockito.Mockito.verify;

import com.hufs.capstone.backend.link.application.RoomLinkCleanupWriteService;
import com.hufs.capstone.backend.room.application.event.RoomDeletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomDeletedEventListenerTest {

	@Mock
	private RoomLinkCleanupWriteService roomLinkCleanupWriteService;

	@InjectMocks
	private RoomDeletedEventListener roomDeletedEventListener;

	@Test
	void onRoomDeletedShouldTriggerCleanup() {
		RoomDeletedEvent event = new RoomDeletedEvent(1L, "room-public-id");

		roomDeletedEventListener.onRoomDeleted(event);

		verify(roomLinkCleanupWriteService).deleteAllByRoomId(1L);
	}
}
