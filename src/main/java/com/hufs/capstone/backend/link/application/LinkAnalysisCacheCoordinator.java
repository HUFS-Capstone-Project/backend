package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class LinkAnalysisCacheCoordinator {

	private static final Duration BURST_CACHE_TTL = Duration.ofSeconds(2);

	private final ConcurrentHashMap<Long, CachedAnalysisEntry> analysisCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, CompletableFuture<LinkAnalysisResult>> inFlight = new ConcurrentHashMap<>();

	public LinkAnalysisResult getOrLoad(Long linkId, Supplier<LinkAnalysisResult> loader) {
		LinkAnalysisResult cached = readCache(linkId);
		if (cached != null) {
			return cached;
		}

		CompletableFuture<LinkAnalysisResult> newFuture = new CompletableFuture<>();
		CompletableFuture<LinkAnalysisResult> existingFuture = inFlight.putIfAbsent(linkId, newFuture);
		if (existingFuture != null) {
			return awaitExisting(existingFuture);
		}

		try {
			LinkAnalysisResult result = loader.get();
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

	private LinkAnalysisResult awaitExisting(CompletableFuture<LinkAnalysisResult> existingFuture) {
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

	private LinkAnalysisResult readCache(Long linkId) {
		CachedAnalysisEntry cached = analysisCache.get(linkId);
		if (cached == null) {
			return null;
		}
		if (Instant.now().isAfter(cached.expiresAt())) {
			analysisCache.remove(linkId, cached);
			return null;
		}
		return cached.result();
	}

	private void cache(Long linkId, LinkAnalysisResult result) {
		analysisCache.put(linkId, new CachedAnalysisEntry(result, Instant.now().plus(BURST_CACHE_TTL)));
	}

	private record CachedAnalysisEntry(LinkAnalysisResult result, Instant expiresAt) {
	}
}
