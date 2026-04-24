package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkAnalysisStatusService {

	private final LinkAnalysisAuthorizationService linkAnalysisAuthorizationService;
	private final LinkAnalysisCacheCoordinator linkAnalysisCacheCoordinator;
	private final LinkRepository linkRepository;
	private final LinkSyncOrchestrator linkSyncOrchestrator;
	private final LinkAnalysisStatusResolver linkAnalysisStatusResolver;
	private final LinkAnalysisStatusWriteService linkAnalysisStatusWriteService;

	public LinkAnalysisResult getLinkAnalysisResult(Long userId, String roomId, Long linkId) {
		linkAnalysisAuthorizationService.assertReadable(userId, roomId, linkId);
		return linkAnalysisCacheCoordinator.getOrLoad(linkId, () -> resolveCurrentStatus(linkId));
	}

	private LinkAnalysisResult resolveCurrentStatus(Long linkId) {
		Link snapshot = linkRepository.findById(linkId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "링크를 찾을 수 없습니다."));

		LinkSyncOrchestrator.ProcessingSyncSnapshot syncSnapshot =
				snapshot.isTerminal() ? null : linkSyncOrchestrator.resolve(snapshot);

		LinkAnalysisStatusResolver.Resolution resolution = linkAnalysisStatusResolver.resolve(snapshot, syncSnapshot);
		if (!resolution.requiresWrite()) {
			return LinkAnalysisResult.from(snapshot);
		}
		return linkAnalysisStatusWriteService.applySyncSnapshot(
				linkId,
				resolution.targetStatus(),
				resolution.captionRaw(),
				resolution.errorCode(),
				resolution.errorMessage()
		);
	}
}
