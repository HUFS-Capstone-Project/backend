package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.SaveManualRoomPlaceCommand;
import com.hufs.capstone.backend.link.application.dto.SaveRoomPlacesCommand;
import com.hufs.capstone.backend.place.application.RoomPlaceDuplicateRaceException;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceSaveResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomPlaceCommandService {

	private static final int MAX_DUPLICATE_RACE_RETRY = 3;

	private final RoomPlaceCommandWriteService roomPlaceCommandWriteService;

	public RoomPlaceSaveResult saveRoomPlaces(
			Long userId,
			String roomId,
			Long analysisRequestId,
			SaveRoomPlacesCommand command
	) {
		return executeWithRaceRetry(
				"candidate",
				roomId,
				analysisRequestId,
				() -> roomPlaceCommandWriteService.saveRoomPlacesWithinTransaction(
						userId,
						roomId,
						analysisRequestId,
						command
				)
		);
	}

	public RoomPlaceSaveResult saveManualRoomPlace(
			Long userId,
			String roomId,
			Long analysisRequestId,
			SaveManualRoomPlaceCommand command
	) {
		return executeWithRaceRetry(
				"manual",
				roomId,
				analysisRequestId,
				() -> roomPlaceCommandWriteService.saveManualRoomPlaceWithinTransaction(
						userId,
						roomId,
						analysisRequestId,
						command
				)
		);
	}

	private RoomPlaceSaveResult executeWithRaceRetry(
			String saveType,
			String roomId,
			Long analysisRequestId,
			SaveOperation operation
	) {
		RuntimeException lastRace = null;
		for (int attempt = 1; attempt <= MAX_DUPLICATE_RACE_RETRY; attempt++) {
			try {
				return operation.execute();
			} catch (RoomPlaceDuplicateRaceException | RoomPlaceCommandWriteService.RoomLinkDuplicateRaceException ex) {
				lastRace = ex;
				log.info(
						"Room place save race detected. type={}, roomId={}, analysisRequestId={}, attempt={}/{}",
						saveType,
						roomId,
						analysisRequestId,
						attempt,
						MAX_DUPLICATE_RACE_RETRY
				);
			}
		}
		throw new BusinessException(ErrorCode.E409_CONFLICT, "Room place save conflict occurred.", lastRace);
	}

	@FunctionalInterface
	private interface SaveOperation {

		RoomPlaceSaveResult execute();
	}
}
