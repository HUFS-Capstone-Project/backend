package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.LinkStatusResult;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkQueryService {

	private final LinkAuthorizationService linkAuthorizationService;
	private final LinkCacheCoordinator linkCacheCoordinator;
	private final LinkRepository linkRepository;
	private final LinkSyncOrchestrator linkSyncOrchestrator;
	private final LinkStatusResolver linkStatusResolver;
	private final LinkStatusWriteService linkStatusWriteService;

	public LinkStatusResult getLinkStatus(Long userId, Long linkId) {
		linkAuthorizationService.assertReadable(userId, linkId);
		return linkCacheCoordinator.getOrLoad(linkId, () -> resolveCurrentStatus(linkId));
	}

	private LinkStatusResult resolveCurrentStatus(Long linkId) {
		Link snapshot = linkRepository.findById(linkId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "링크를 찾을 수 없습니다."));

		LinkSyncOrchestrator.ProcessingSyncSnapshot syncSnapshot =
				snapshot.isTerminal() ? null : linkSyncOrchestrator.resolve(snapshot);

		LinkStatusResolver.Resolution resolution = linkStatusResolver.resolve(snapshot, syncSnapshot);
		if (!resolution.requiresWrite()) {
			return LinkStatusResult.from(snapshot);
		}
		return linkStatusWriteService.applySyncSnapshot(linkId, resolution.targetStatus(), resolution.captionRaw());
	}
}

