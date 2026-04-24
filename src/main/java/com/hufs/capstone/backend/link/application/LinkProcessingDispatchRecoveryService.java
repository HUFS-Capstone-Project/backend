package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.application.event.LinkProcessingRequestedEvent;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkAnalysisRequest;
import com.hufs.capstone.backend.link.domain.repository.LinkAnalysisRequestRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkProcessingDispatchRecoveryService {

	private final LinkRepository linkRepository;
	private final LinkAnalysisRequestRepository linkAnalysisRequestRepository;
	private final LinkProcessingDispatchPolicy dispatchPolicy;

	@Transactional(readOnly = true)
	public List<LinkProcessingRequestedEvent> findRecoverableEvents(Instant now) {
		Instant staleBefore = now.minus(dispatchPolicy.getStaleThreshold());
		List<Link> staleLinks = linkRepository.findStaleDispatchTargets(
				Set.of(ProcessingDispatchStatus.PENDING, ProcessingDispatchStatus.DISPATCHING),
				LinkAnalysisStatus.REQUESTED,
				staleBefore,
				PageRequest.of(0, dispatchPolicy.getRecoveryBatchSize())
		);
		return staleLinks.stream()
				.map(this::toRecoveryEvent)
				.flatMap(Optional::stream)
				.toList();
	}

	private Optional<LinkProcessingRequestedEvent> toRecoveryEvent(Link link) {
		return linkAnalysisRequestRepository.findFirstByLinkIdOrderByIdAsc(link.getId())
				.map(request -> toEvent(link, request))
				.or(() -> {
					log.warn("분석 요청 이력이 없어 stale dispatch 복구를 건너뜁니다. linkId={}", link.getId());
					return Optional.empty();
				});
	}

	private LinkProcessingRequestedEvent toEvent(Link link, LinkAnalysisRequest request) {
		return new LinkProcessingRequestedEvent(
				link.getId(),
				link.getNormalizedUrl(),
				request.getRoom().getPublicId(),
				request.getSource()
		);
	}
}
