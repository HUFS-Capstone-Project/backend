package com.hufs.capstone.backend.room.application;

import com.hufs.capstone.backend.room.application.port.RoomDataCleanupPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomOwnedDataCleanupService {

	private final List<RoomDataCleanupPort> cleanupPorts;

	@Transactional(propagation = Propagation.MANDATORY)
	public void deleteAllByRoomId(Long roomId) {
		cleanupPorts.forEach(cleanupPort -> cleanupPort.deleteAllByRoomId(roomId));
	}
}
