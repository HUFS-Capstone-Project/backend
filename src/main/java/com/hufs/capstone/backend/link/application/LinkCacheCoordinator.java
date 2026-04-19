package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.application.dto.LinkStatusResult;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class LinkCacheCoordinator {

	private static final Duration BURST_CACHE_TTL = Duration.ofSeconds(2);

	private final ConcurrentHashMap<Long, CachedStatusEntry> statusCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, CompletableFuture<LinkStatusResult>> inFlight = new ConcurrentHashMap<>();

	public LinkStatusResult getOrLoad(Long linkId, Supplier<LinkStatusResult> loader) {
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
			LinkStatusResult result = loader.get();
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

	private record CachedStatusEntry(LinkStatusResult result, Instant expiresAt) {
	}
}
