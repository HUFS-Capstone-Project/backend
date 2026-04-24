package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.AnalyzeLinkCommand;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisRequestResult;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkAnalysisRequestService {

	private final LinkAnalysisRequestWriteService linkAnalysisRequestWriteService;
	private final LinkRepository linkRepository;

	public LinkAnalysisRequestResult requestLinkAnalysis(Long userId, String roomId, AnalyzeLinkCommand command) {
		String requiredRoomId = requireRoomId(roomId);
		String source = command.source() == null ? null : command.source().name();
		LinkUrlNormalizer.NormalizedUrl normalizedUrl = LinkUrlNormalizer.normalize(command.url());

		LinkAnalysisRequestResult requested;
		try {
			requested = linkAnalysisRequestWriteService.requestWithinWriteTransaction(
					normalizedUrl,
					requiredRoomId,
					userId,
					source
			);
		} catch (LinkAnalysisRequestWriteService.LinkDuplicateRaceException ex) {
			log.info(
					"정규화 URL 중복 경합 이후 링크 분석 요청을 재시도합니다. normalizedUrl={}",
					ex.normalizedUrl()
			);
			requested = linkAnalysisRequestWriteService.requestWithinWriteTransaction(
					normalizedUrl,
					requiredRoomId,
					userId,
					source
			);
		}

		LinkAnalysisRequestResult resolved = refreshLatest(requested);
		log.info(
				"링크 분석 요청이 완료되었습니다. linkId={}, roomId={}, jobId={}, status={}, createdRequest={}",
				resolved.linkId(),
				requiredRoomId,
				resolved.processingJobId(),
				resolved.status(),
				resolved.createdRequest()
		);
		return resolved;
	}

	private LinkAnalysisRequestResult refreshLatest(LinkAnalysisRequestResult requested) {
		return linkRepository.findById(requested.linkId())
				.map(link -> LinkAnalysisRequestResult.from(link, requested.createdRequest()))
				.orElse(requested);
	}

	private static String requireRoomId(String roomId) {
		if (roomId == null || roomId.isBlank()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "방 ID는 필수입니다.");
		}
		return roomId.trim();
	}
}
