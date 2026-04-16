package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.external.processing.ProcessingClient;
import com.hufs.capstone.backend.external.processing.dto.CreateProcessingJobResponse;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkCommand;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkResult;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkProcessingHistory;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.link.domain.repository.LinkProcessingHistoryRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkCommandService {

	private final LinkRepository linkRepository;
	private final RoomLinkRepository roomLinkRepository;
	private final LinkProcessingHistoryRepository linkProcessingHistoryRepository;
	private final ProcessingClient processingClient;
	private final TransactionOperations transactionOperations;

	public RegisterLinkResult register(RegisterLinkCommand command) {
		String roomId = requireRoomId(command.roomId());
		String source = trimToNull(command.source());
		LinkUrlNormalizer.NormalizedUrl normalizedUrl = LinkUrlNormalizer.normalize(command.url());

		String newProcessingJobId = null;
		if (linkRepository.findByNormalizedUrl(normalizedUrl.normalizedUrl()).isEmpty()) {
			newProcessingJobId = createJob(normalizedUrl, roomId, source).jobId();
		}
		final String processingJobIdForCreate = newProcessingJobId;

		RegisterLinkResult result;
		try {
			result = executeWriteTransaction(normalizedUrl, roomId, source, processingJobIdForCreate);
		} catch (LinkDuplicateRaceException ex) {
			log.info(
					"Retrying link registration after normalized URL duplicate race. normalizedUrl={}",
					ex.normalizedUrl()
			);
			result = executeWriteTransaction(normalizedUrl, roomId, source, null);
		}

		if (result == null) {
			throw new BusinessException(ErrorCode.E500_INTERNAL, "Failed to register link.");
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

	private RegisterLinkResult executeWriteTransaction(
			LinkUrlNormalizer.NormalizedUrl normalizedUrl,
			String roomId,
			String source,
			String newProcessingJobId
	) {
		return transactionOperations.execute(
				status -> registerWithinWriteTransaction(normalizedUrl, roomId, source, newProcessingJobId)
		);
	}

	private CreateProcessingJobResponse createJob(
			LinkUrlNormalizer.NormalizedUrl normalizedUrl,
			String roomId,
			String source
	) {
		return processingClient.createJob(normalizedUrl.normalizedUrl(), roomId, source);
	}

	private RegisterLinkResult registerWithinWriteTransaction(
			LinkUrlNormalizer.NormalizedUrl normalizedUrl,
			String roomId,
			String source,
			String newProcessingJobId
	) {
		Link link = linkRepository.findByNormalizedUrl(normalizedUrl.normalizedUrl())
				.orElseGet(() -> persistNewLink(normalizedUrl, newProcessingJobId));

		bindRoomLink(link, roomId);
		saveRegisteredHistory(link, roomId, source);
		return new RegisterLinkResult(link.getId(), link.getProcessingJobId(), link.getStatus());
	}

	private Link persistNewLink(LinkUrlNormalizer.NormalizedUrl normalizedUrl, String processingJobId) {
		if (processingJobId == null || processingJobId.isBlank()) {
			throw new BusinessException(
					ErrorCode.E500_INTERNAL,
					"Processing job id is required for creating a new link."
			);
		}
		return persistLinkWithDuplicateGuard(normalizedUrl, processingJobId);
	}

	protected Link persistLinkWithDuplicateGuard(LinkUrlNormalizer.NormalizedUrl normalizedUrl, String processingJobId) {
		Link newLink = Link.register(normalizedUrl.originalUrl(), normalizedUrl.normalizedUrl(), processingJobId);
		try {
			return linkRepository.saveAndFlush(newLink);
		} catch (DataIntegrityViolationException ex) {
			throw new LinkDuplicateRaceException(normalizedUrl.normalizedUrl(), ex);
		} catch (DataAccessException ex) {
			log.error("Failed to persist link. normalizedUrl={}", normalizedUrl.normalizedUrl(), ex);
			throw new BusinessException(ErrorCode.E500_INTERNAL, "Failed to persist link.", ex);
		}
	}

	protected void bindRoomLink(Link link, String roomId) {
		try {
			roomLinkRepository.saveAndFlush(RoomLink.bind(roomId, link));
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException(ErrorCode.E409_CONFLICT, "Link is already saved in this room.");
		} catch (DataAccessException ex) {
			log.error("Failed to save room link mapping. roomId={}, linkId={}", roomId, link.getId(), ex);
			throw new BusinessException(ErrorCode.E500_INTERNAL, "Failed to save room link mapping.", ex);
		}
	}

	private void saveRegisteredHistory(Link link, String roomId, String source) {
		try {
			linkProcessingHistoryRepository.saveAndFlush(LinkProcessingHistory.registered(link, roomId, source));
		} catch (DataAccessException ex) {
			log.warn("Failed to save link registered history. linkId={}, roomId={}", link.getId(), roomId, ex);
		}
	}

	private static String requireRoomId(String roomId) {
		String normalized = trimToNull(roomId);
		if (normalized == null) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "Room ID is required.");
		}
		return normalized;
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static final class LinkDuplicateRaceException extends RuntimeException {

		private final String normalizedUrl;

		private LinkDuplicateRaceException(String normalizedUrl, Throwable cause) {
			super("Normalized URL duplicate race detected: " + normalizedUrl, cause);
			this.normalizedUrl = normalizedUrl;
		}

		private String normalizedUrl() {
			return normalizedUrl;
		}
	}
}
