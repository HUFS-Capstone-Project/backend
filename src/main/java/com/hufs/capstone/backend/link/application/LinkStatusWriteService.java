package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.LinkStatusResult;
import com.hufs.capstone.backend.link.application.event.LinkStatusSyncedEvent;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkStatusWriteService {

	private static final Set<LinkAnalysisStatus> UPDATABLE_STATUSES =
			Set.of(LinkAnalysisStatus.REQUESTED, LinkAnalysisStatus.PROCESSING);
	private static final int MAX_CAS_RETRY = 3;

	private final LinkRepository linkRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public LinkStatusResult applySyncSnapshot(Long linkId, LinkAnalysisStatus targetStatus, String captionRaw) {
		for (int retry = 0; retry < MAX_CAS_RETRY; retry++) {
			Link current = linkRepository.findById(linkId)
					.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "링크를 찾을 수 없습니다."));

			if (current.isTerminal()) {
				return LinkStatusResult.from(current);
			}

			CasPlan plan = CasPlan.from(current, targetStatus, captionRaw);
			if (!plan.changed()) {
				return LinkStatusResult.from(current);
			}

			int updated = executeCasUpdate(current.getId(), current.getVersion(), plan);
			if (updated == 1) {
				Link refreshed = linkRepository.findById(linkId)
						.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "링크를 찾을 수 없습니다."));
				eventPublisher.publishEvent(new LinkStatusSyncedEvent(refreshed.getId()));
				return LinkStatusResult.from(refreshed);
			}
		}

		log.warn("CAS 업데이트 경합이 발생해 최신 스냅샷을 반환합니다. linkId={}, targetStatus={}", linkId, targetStatus);
		Link latest = linkRepository.findById(linkId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "링크를 찾을 수 없습니다."));
		return LinkStatusResult.from(latest);
	}

	private int executeCasUpdate(Long linkId, Long expectedVersion, CasPlan plan) {
		Instant now = Instant.now();
		if (plan.updateCaption()) {
			return linkRepository.compareAndSetStatusAndCaption(
					linkId,
					expectedVersion,
					UPDATABLE_STATUSES,
					plan.targetStatus(),
					plan.captionRaw(),
					now
			);
		}
		return linkRepository.compareAndSetStatus(
				linkId,
				expectedVersion,
				UPDATABLE_STATUSES,
				plan.targetStatus(),
				now
		);
	}

	private record CasPlan(
			boolean changed,
			LinkAnalysisStatus targetStatus,
			boolean updateCaption,
			String captionRaw
	) {

		private static CasPlan from(Link current, LinkAnalysisStatus targetStatus, String captionRaw) {
			return switch (targetStatus) {
				case REQUESTED -> requestedPlan(current);
				case PROCESSING -> processingPlan(current);
				case FAILED -> failedPlan(current);
				case SUCCEEDED -> succeededPlan(current, captionRaw);
			};
		}

		private static CasPlan requestedPlan(Link current) {
			if (current.getStatus() == LinkAnalysisStatus.REQUESTED || current.getStatus() == LinkAnalysisStatus.PROCESSING) {
				return unchanged(current.getStatus(), false, null);
			}
			return changed(LinkAnalysisStatus.REQUESTED, false, null);
		}

		private static CasPlan processingPlan(Link current) {
			if (current.getStatus() == LinkAnalysisStatus.PROCESSING) {
				return unchanged(current.getStatus(), false, null);
			}
			return changed(LinkAnalysisStatus.PROCESSING, false, null);
		}

		private static CasPlan failedPlan(Link current) {
			if (current.getStatus() == LinkAnalysisStatus.FAILED || current.getStatus() == LinkAnalysisStatus.SUCCEEDED) {
				return unchanged(current.getStatus(), false, null);
			}
			return changed(LinkAnalysisStatus.FAILED, false, null);
		}

		private static CasPlan succeededPlan(Link current, String captionRaw) {
			boolean changedStatus = current.getStatus() != LinkAnalysisStatus.SUCCEEDED;
			boolean changedCaption = !Objects.equals(current.getCaptionRaw(), captionRaw);
			if (!changedStatus && !changedCaption) {
				return unchanged(current.getStatus(), false, current.getCaptionRaw());
			}
			return changed(LinkAnalysisStatus.SUCCEEDED, true, captionRaw);
		}

		private static CasPlan unchanged(LinkAnalysisStatus status, boolean updateCaption, String captionRaw) {
			return new CasPlan(false, status, updateCaption, captionRaw);
		}

		private static CasPlan changed(LinkAnalysisStatus status, boolean updateCaption, String captionRaw) {
			return new CasPlan(true, status, updateCaption, captionRaw);
		}
	}
}

