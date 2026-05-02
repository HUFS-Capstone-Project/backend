package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.RoomPlaceSaveResult;
import com.hufs.capstone.backend.link.application.dto.SaveRoomPlacesCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomPlaceCommandService {

	private static final int MAX_DUPLICATE_RACE_RETRY = 3;

	private final RoomPlaceCommandWriteService roomPlaceCommandWriteService;

	public RoomPlaceSaveResult saveRoomPlaces(Long userId, String roomId, Long linkId, SaveRoomPlacesCommand command) {
		RuntimeException lastRace = null;
		for (int attempt = 1; attempt <= MAX_DUPLICATE_RACE_RETRY; attempt++) {
			try {
				return roomPlaceCommandWriteService.saveRoomPlacesWithinTransaction(userId, roomId, linkId, command);
			} catch (RoomPlaceCommandWriteService.RoomPlaceDuplicateRaceException ex) {
				lastRace = ex;
				log.info(
						"Room place duplicate race detected. roomId={}, linkId={}, attempt={}/{}",
						roomId,
						linkId,
						attempt,
						MAX_DUPLICATE_RACE_RETRY
				);
			}
		}
		throw new BusinessException(ErrorCode.E409_CONFLICT, "Room place save conflict occurred.", lastRace);
	}
}
