package com.hufs.capstone.backend.link.domain.repository;

import com.hufs.capstone.backend.link.domain.entity.LinkProcessingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkProcessingHistoryRepository extends JpaRepository<LinkProcessingHistory, Long> {

	long countByLinkId(Long linkId);
}
