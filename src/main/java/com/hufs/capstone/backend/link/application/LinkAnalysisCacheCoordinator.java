package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.cache.CacheNames;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class LinkAnalysisCacheCoordinator {

	private final Cache analysisCache;
	private final ConcurrentHashMap<Long, InFlightLoad> inFlight = new ConcurrentHashMap<>();
	private final long waitTimeoutMillis;

	@Autowired
	public LinkAnalysisCacheCoordinator(
			CacheManager cacheManager,
			@Value("${app.cache.link-analysis.wait-timeout:35s}") Duration waitTimeout
	) {
		this(requireAnalysisCache(cacheManager), waitTimeout);
	}

	LinkAnalysisCacheCoordinator(Cache analysisCache) {
		this(analysisCache, Duration.ofSeconds(35));
	}

	LinkAnalysisCacheCoordinator(Cache analysisCache, Duration waitTimeout) {
		this.analysisCache = Objects.requireNonNull(analysisCache, "analysisCache must not be null");
		Objects.requireNonNull(waitTimeout, "waitTimeout must not be null");
		if (waitTimeout.isZero() || waitTimeout.isNegative()) {
			throw new IllegalArgumentException("waitTimeout must be positive");
		}
		this.waitTimeoutMillis = waitTimeout.toMillis();
		if (waitTimeoutMillis == 0L) {
			throw new IllegalArgumentException("waitTimeout must be at least 1 millisecond");
		}
	}

	public LinkAnalysisResult getOrLoad(Long linkId, Supplier<LinkAnalysisResult> loader) {
		LinkAnalysisResult cached = analysisCache.get(linkId, LinkAnalysisResult.class);
		if (cached != null) {
			return cached;
		}

		InFlightLoad newLoad = new InFlightLoad();
		InFlightLoad existingLoad = inFlight.putIfAbsent(linkId, newLoad);
		if (existingLoad != null) {
			return awaitExisting(existingLoad, waitTimeoutMillis);
		}

		try {
			LinkAnalysisResult filledWhileRegistering = analysisCache.get(linkId, LinkAnalysisResult.class);
			if (filledWhileRegistering != null) {
				newLoad.future.complete(filledWhileRegistering);
				return filledWhileRegistering;
			}
			LinkAnalysisResult result = loader.get();
			analysisCache.put(linkId, result);
			newLoad.future.complete(result);
			return result;
		} catch (RuntimeException | Error ex) {
			newLoad.future.completeExceptionally(ex);
			throw ex;
		} finally {
			inFlight.remove(linkId, newLoad);
		}
	}

	private static LinkAnalysisResult awaitExisting(
			InFlightLoad existingLoad,
			long timeoutMillis
	) {
		existingLoad.waiters.incrementAndGet();
		try {
			return existingLoad.future.get(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for link analysis cache load", ex);
		} catch (TimeoutException ex) {
			throw new IllegalStateException("Timed out while waiting for link analysis cache load", ex);
		} catch (java.util.concurrent.ExecutionException ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new CompletionException(cause);
		} catch (CompletionException ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw ex;
		} finally {
			existingLoad.waiters.decrementAndGet();
		}
	}

	int waitingCallerCount(Long linkId) {
		InFlightLoad load = inFlight.get(linkId);
		return load == null ? 0 : load.waiters.get();
	}

	private static Cache requireAnalysisCache(CacheManager cacheManager) {
		Cache cache = cacheManager.getCache(CacheNames.LINK_ANALYSIS_RESULTS);
		if (cache == null) {
			throw new IllegalStateException("Missing cache: " + CacheNames.LINK_ANALYSIS_RESULTS);
		}
		return cache;
	}

	private static final class InFlightLoad {

		private final CompletableFuture<LinkAnalysisResult> future = new CompletableFuture<>();
		private final AtomicInteger waiters = new AtomicInteger();
	}
}
