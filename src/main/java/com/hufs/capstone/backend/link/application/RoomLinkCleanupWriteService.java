package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomLinkCleanupWriteService {

	private final RoomLinkRepository roomLinkRepository;

	@Transactional(propagation = Propagation.MANDATORY)
	public void deleteAllByRoomId(Long roomId) {
		roomLinkRepository.deleteByRoomId(roomId);
	}
}
