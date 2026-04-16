package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.external.processing.ProcessingClient;
import com.hufs.capstone.backend.external.processing.dto.CreateProcessingJobResponse;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkCommand;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkResult;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkCommandService {

	private final LinkRepository linkRepository;
	private final ProcessingClient processingClient;
	private final RoomAccessService roomAccessService;
	private final LinkRegistrationWriteService linkRegistrationWriteService;

	public RegisterLinkResult register(Long userId, RegisterLinkCommand command) {
		String roomId = requireRoomId(command.roomId());
		String source = command.source() == null ? null : command.source().name();
		LinkUrlNormalizer.NormalizedUrl normalizedUrl = LinkUrlNormalizer.normalize(command.url());
		Room memberRoom = roomAccessService.requireMemberRoom(roomId, userId);

		String newProcessingJobId = null;
		if (linkRepository.findByNormalizedUrl(normalizedUrl.normalizedUrl()).isEmpty()) {
			newProcessingJobId = createJob(normalizedUrl, memberRoom.getPublicId(), source).jobId();
		}

		RegisterLinkResult result;
		try {
			result = linkRegistrationWriteService.registerWithinWriteTransaction(
					normalizedUrl,
					memberRoom.getPublicId(),
					userId,
					source,
					newProcessingJobId
			);
		} catch (LinkRegistrationWriteService.LinkDuplicateRaceException ex) {
			log.info(
					"Retrying link registration after normalized URL duplicate race. normalizedUrl={}",
					ex.normalizedUrl()
			);
			result = linkRegistrationWriteService.registerWithinWriteTransaction(
					normalizedUrl,
					memberRoom.getPublicId(),
					userId,
					source,
					null
			);
		}

		log.info(
				"Link registered. linkId={}, roomId={}, jobId={}, status={}",
				result.linkId(),
				roomId,
				result.processingJobId(),
				result.status()
		);
		return result;
	}

	private CreateProcessingJobResponse createJob(
			LinkUrlNormalizer.NormalizedUrl normalizedUrl,
			String roomId,
			String source
	) {
		return processingClient.createJob(normalizedUrl.normalizedUrl(), roomId, source);
	}

	private static String requireRoomId(String roomId) {
		if (roomId == null || roomId.isBlank()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "방 ID는 필수입니다.");
		}
		return roomId.trim();
	}
}
