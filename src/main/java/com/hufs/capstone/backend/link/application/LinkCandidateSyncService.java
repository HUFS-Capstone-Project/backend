package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkCandidate;
import com.hufs.capstone.backend.link.domain.repository.LinkCandidateRepository;
import com.hufs.capstone.backend.link.domain.vo.PlaceCandidateSnapshot;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkCandidateSyncService {

	private final LinkCandidateRepository linkCandidateRepository;

	public void replaceCandidates(Link link, List<PlaceCandidateSnapshot> snapshots) {
		linkCandidateRepository.deleteByLinkId(link.getId());
		if (snapshots == null || snapshots.isEmpty()) {
			return;
		}
		List<LinkCandidate> candidates = new ArrayList<>(snapshots.size());
		for (int index = 0; index < snapshots.size(); index++) {
			candidates.add(LinkCandidate.create(link, index, snapshots.get(index)));
		}
		linkCandidateRepository.saveAll(candidates);
	}
}
