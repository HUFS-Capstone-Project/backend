package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkCommand;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkResult;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkCommandService {

	private final LinkRegistrationWriteService linkRegistrationWriteService;
	private final LinkRepository linkRepository;

	public RegisterLinkResult register(Long userId, RegisterLinkCommand command) {
		String roomId = requireRoomId(command.roomId());
		String source = command.source() == null ? null : command.source().name();
		LinkUrlNormalizer.NormalizedUrl normalizedUrl = LinkUrlNormalizer.normalize(command.url());

		RegisterLinkResult registered;
		try {
			registered = linkRegistrationWriteService.registerWithinWriteTransaction(
					normalizedUrl,
					roomId,
					userId,
					source
			);
		} catch (LinkRegistrationWriteService.LinkDuplicateRaceException ex) {
			log.info(
					"정규화 URL 중복 경합 이후 링크 등록을 재시도합니다. normalizedUrl={}",
					ex.normalizedUrl()
			);
			registered = linkRegistrationWriteService.registerWithinWriteTransaction(
					normalizedUrl,
					roomId,
					userId,
					source
			);
		}

		RegisterLinkResult resolved = resolveLatestIfPending(registered);
		log.info(
				"링크 등록이 완료되었습니다. linkId={}, roomId={}, jobId={}, status={}, dispatchStatus={}",
				resolved.linkId(),
				roomId,
				resolved.processingJobId(),
				resolved.status(),
				resolved.dispatchStatus()
		);
		return resolved;
	}

	private RegisterLinkResult resolveLatestIfPending(RegisterLinkResult registered) {
		if (registered.dispatchStatus() != ProcessingDispatchStatus.PENDING) {
			return registered;
		}
		return linkRepository.findById(registered.linkId())
				.map(RegisterLinkResult::from)
				.orElse(registered);
	}

	private static String requireRoomId(String roomId) {
		if (roomId == null || roomId.isBlank()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "방 ID는 필수입니다.");
		}
		return roomId.trim();
	}
}

