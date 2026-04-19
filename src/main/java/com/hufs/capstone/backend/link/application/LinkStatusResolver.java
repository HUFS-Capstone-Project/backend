package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import org.springframework.stereotype.Service;

@Service
public class LinkStatusResolver {

	public Resolution resolve(Link snapshot, LinkSyncOrchestrator.ProcessingSyncSnapshot syncSnapshot) {
		if (snapshot.isTerminal()) {
			return Resolution.noWrite();
		}
		if (syncSnapshot == null) {
			throw new IllegalArgumentException("syncSnapshot is required for non-terminal link.");
		}
		return Resolution.write(syncSnapshot.status(), syncSnapshot.captionRaw());
	}

	public record Resolution(
			boolean requiresWrite,
			LinkAnalysisStatus targetStatus,
			String captionRaw
	) {

		public static Resolution noWrite() {
			return new Resolution(false, null, null);
		}

		public static Resolution write(LinkAnalysisStatus targetStatus, String captionRaw) {
			return new Resolution(true, targetStatus, captionRaw);
		}
	}
}
