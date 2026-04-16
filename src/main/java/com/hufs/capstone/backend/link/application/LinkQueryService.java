package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.external.processing.ProcessingClient;
import com.hufs.capstone.backend.external.processing.ProcessingClientException;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResponse;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResultResponse;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.LinkStatusResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkQueryService {

	private static final int RESULT_NOT_READY_STATUS = HttpStatus.NOT_FOUND.value();
	private static final Duration BURST_CACHE_TTL = Duration.ofSeconds(2);

	private final LinkRepository linkRepository;
	private final ProcessingClient processingClient;
	private final LinkStatusWriteService linkStatusWriteService;

	private final ConcurrentHashMap<Long, CachedStatusEntry> statusCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, CompletableFuture<LinkStatusResult>> inFlight = new ConcurrentHashMap<>();

	public LinkStatusResult getLinkStatus(Long linkId) {
		LinkStatusResult cached = readCache(linkId);
		if (cached != null) {
			return cached;
		}

		CompletableFuture<LinkStatusResult> newFuture = new CompletableFuture<>();
		CompletableFuture<LinkStatusResult> existingFuture = inFlight.putIfAbsent(linkId, newFuture);
		if (existingFuture != null) {
			return awaitExisting(existingFuture);
		}

		try {
			LinkStatusResult result = fetchAndSync(linkId);
			cache(linkId, result);
			newFuture.complete(result);
			return result;
		} catch (RuntimeException ex) {
			newFuture.completeExceptionally(ex);
			throw ex;
		} finally {
			inFlight.remove(linkId, newFuture);
		}
	}

	private LinkStatusResult fetchAndSync(Long linkId) {
		Link snapshot = linkRepository.findById(linkId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "Link not found."));

		if (snapshot.isTerminal()) {
			return LinkStatusResult.from(snapshot);
		}

		ProcessingSyncSnapshot syncSnapshot = resolveProcessingSnapshot(snapshot);
		return linkStatusWriteService.applySyncSnapshot(linkId, syncSnapshot.status(), syncSnapshot.captionRaw());
	}

	private LinkStatusResult awaitExisting(CompletableFuture<LinkStatusResult> existingFuture) {
		try {
			return existingFuture.join();
		} catch (CompletionException ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw ex;
		}
	}

	private LinkStatusResult readCache(Long linkId) {
		CachedStatusEntry cached = statusCache.get(linkId);
		if (cached == null) {
			return null;
		}
		if (Instant.now().isAfter(cached.expiresAt())) {
			statusCache.remove(linkId, cached);
			return null;
		}
		return cached.result();
	}

	private void cache(Long linkId, LinkStatusResult result) {
		statusCache.put(linkId, new CachedStatusEntry(result, Instant.now().plus(BURST_CACHE_TTL)));
	}

	private ProcessingSyncSnapshot resolveProcessingSnapshot(Link link) {
		ProcessingJobResponse jobResponse = processingClient.getJob(link.getProcessingJobId());
		LinkAnalysisStatus processingStatus = LinkAnalysisStatus.fromProcessingStatus(jobResponse.status());

		return switch (processingStatus) {
			case SUCCEEDED -> resolveSucceededSnapshot(link.getProcessingJobId());
			case FAILED -> ProcessingSyncSnapshot.failed();
			case REQUESTED -> ProcessingSyncSnapshot.requested();
			case PROCESSING -> ProcessingSyncSnapshot.processing();
		};
	}

	private ProcessingSyncSnapshot resolveSucceededSnapshot(String jobId) {
		ProcessingJobResultResponse resultResponse = getJobResultOrNull(jobId);
		if (resultResponse == null) {
			return ProcessingSyncSnapshot.processing();
		}
		String caption = trimToNull(resultResponse.caption());
		if (caption == null) {
			log.warn("Processing result returned without caption. keeping PROCESSING. jobId={}", jobId);
			return ProcessingSyncSnapshot.processing();
		}
		return ProcessingSyncSnapshot.succeeded(caption);
	}

	private ProcessingJobResultResponse getJobResultOrNull(String jobId) {
		try {
			return processingClient.getJobResult(jobId);
		} catch (ProcessingClientException ex) {
			if (ex.getStatus() != null && ex.getStatus().value() == RESULT_NOT_READY_STATUS) {
				log.debug("Processing result is not ready yet. jobId={}", jobId);
				return null;
			}
			throw ex;
		}
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record CachedStatusEntry(LinkStatusResult result, Instant expiresAt) {
	}

	private record ProcessingSyncSnapshot(LinkAnalysisStatus status, String captionRaw) {

		private static ProcessingSyncSnapshot requested() {
			return new ProcessingSyncSnapshot(LinkAnalysisStatus.REQUESTED, null);
		}

		private static ProcessingSyncSnapshot processing() {
			return new ProcessingSyncSnapshot(LinkAnalysisStatus.PROCESSING, null);
		}

		private static ProcessingSyncSnapshot failed() {
			return new ProcessingSyncSnapshot(LinkAnalysisStatus.FAILED, null);
		}

		private static ProcessingSyncSnapshot succeeded(String captionRaw) {
			return new ProcessingSyncSnapshot(LinkAnalysisStatus.SUCCEEDED, captionRaw);
		}
	}
}
