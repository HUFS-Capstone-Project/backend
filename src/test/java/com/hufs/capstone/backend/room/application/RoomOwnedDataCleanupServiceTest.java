package com.hufs.capstone.backend.room.application;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.hufs.capstone.backend.room.application.port.RoomDataCleanupPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class RoomOwnedDataCleanupServiceTest {

	@Test
	void deleteAllByRoomIdShouldInvokeCleanupPortsInOrder() {
		RoomDataCleanupPort first = mock(RoomDataCleanupPort.class);
		RoomDataCleanupPort second = mock(RoomDataCleanupPort.class);
		RoomOwnedDataCleanupService cleanupService = new RoomOwnedDataCleanupService(List.of(first, second));

		cleanupService.deleteAllByRoomId(1L);

		InOrder inOrder = inOrder(first, second);
		inOrder.verify(first).deleteAllByRoomId(1L);
		inOrder.verify(second).deleteAllByRoomId(1L);
	}
}
