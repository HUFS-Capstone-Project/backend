package com.hufs.capstone.backend.link.application.event;

import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkProcessingHistory;
import com.hufs.capstone.backend.link.domain.repository.LinkProcessingHistoryRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkStatusHistoryEventListener {

	private final LinkRepository linkRepository;
	private final LinkProcessingHistoryRepository linkProcessingHistoryRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onStatusSynced(LinkStatusSyncedEvent event) {
		try {
			linkRepository.findById(event.linkId()).ifPresentOrElse(
					this::saveStatusSyncedHistory,
					() -> log.warn("링크가 없어 상태 이력 저장을 건너뜁니다. linkId={}", event.linkId())
			);
		} catch (DataAccessException ex) {
			log.warn("상태 동기화 이력 저장에 실패했습니다. linkId={}", event.linkId(), ex);
		}
	}

	private void saveStatusSyncedHistory(Link link) {
		linkProcessingHistoryRepository.save(LinkProcessingHistory.statusSynced(link));
	}
}

