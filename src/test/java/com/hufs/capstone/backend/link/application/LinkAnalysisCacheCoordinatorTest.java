package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.hufs.capstone.backend.global.cache.CacheNames;
import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

class LinkAnalysisCacheCoordinatorTest {

	private static final Duration TTL = Duration.ofSeconds(2);
	private static final int CONCURRENT_CALLERS = 20;

	private final List<ExecutorService> executors = new ArrayList<>();

	@AfterEach
	void tearDown() {
		executors.forEach(ExecutorService::shutdownNow);
	}

	@Test
	void returnsCachedValueWithoutInvokingLoaderAgain() {
		TestCache testCache = newCache(10L);
		AtomicInteger loads = new AtomicInteger();

		LinkAnalysisResult first = testCache.coordinator().getOrLoad(1L, () -> result(1L, loads.incrementAndGet()));
		LinkAnalysisResult second = testCache.coordinator().getOrLoad(1L, () -> result(1L, loads.incrementAndGet()));

		assertThat(second).isSameAs(first);
		assertThat(loads).hasValue(1);
	}

	@Test
	void loadsOnceForConcurrentRequestsWithSameKey() throws Exception {
		TestCache testCache = newCache(10L);
		AtomicInteger loads = new AtomicInteger();
		CountDownLatch loaderEntered = new CountDownLatch(1);
		CountDownLatch releaseLoader = new CountDownLatch(1);
		ExecutorService executor = newExecutor(CONCURRENT_CALLERS);

		Future<LinkAnalysisResult> first = executor.submit(() -> testCache.coordinator().getOrLoad(1L, () -> {
			loads.incrementAndGet();
			loaderEntered.countDown();
			await(releaseLoader);
			return result(1L, 1);
		}));
		assertThat(loaderEntered.await(1, TimeUnit.SECONDS)).isTrue();

		List<Future<LinkAnalysisResult>> waiting = new ArrayList<>();
		for (int caller = 1; caller < CONCURRENT_CALLERS; caller++) {
			waiting.add(executor.submit(() -> testCache.coordinator().getOrLoad(1L, () -> {
				loads.incrementAndGet();
				return result(1L, 2);
			})));
		}
		releaseLoader.countDown();

		assertThat(first.get(1, TimeUnit.SECONDS).contentText()).isEqualTo("load-1");
		for (Future<LinkAnalysisResult> future : waiting) {
			assertThat(future.get(1, TimeUnit.SECONDS).contentText()).isEqualTo("load-1");
		}
		assertThat(loads).hasValue(1);
	}

	@Test
	void rechecksCacheAfterWinningInFlightOwnership() throws Exception {
		StaleFirstReadCache cache = new StaleFirstReadCache();
		LinkAnalysisCacheCoordinator coordinator = new LinkAnalysisCacheCoordinator(cache);
		AtomicInteger loads = new AtomicInteger();
		ExecutorService executor = newExecutor(2);

		Future<LinkAnalysisResult> delayedReader = executor.submit(() -> coordinator.getOrLoad(
				1L,
				() -> result(1L, loads.incrementAndGet())
		));
		assertThat(cache.firstReadObserved.await(1, TimeUnit.SECONDS)).isTrue();
		Future<LinkAnalysisResult> loader = executor.submit(() -> coordinator.getOrLoad(
				1L,
				() -> result(1L, loads.incrementAndGet())
		));

		assertThat(loader.get(1, TimeUnit.SECONDS).contentText()).isEqualTo("load-1");
		assertThat(delayedReader.get(1, TimeUnit.SECONDS).contentText()).isEqualTo("load-1");
		assertThat(loads).hasValue(1);
	}

	@Test
	void loadsDifferentKeysWithoutGlobalSerialization() throws Exception {
		TestCache testCache = newCache(10L);
		CountDownLatch bothLoadersEntered = new CountDownLatch(2);
		CountDownLatch releaseLoaders = new CountDownLatch(1);
		ExecutorService executor = newExecutor(2);

		Future<LinkAnalysisResult> first = executor.submit(() -> loadAfterBarrier(
				testCache.coordinator(), 1L, bothLoadersEntered, releaseLoaders));
		Future<LinkAnalysisResult> second = executor.submit(() -> loadAfterBarrier(
				testCache.coordinator(), 2L, bothLoadersEntered, releaseLoaders));

		assertThat(bothLoadersEntered.await(1, TimeUnit.SECONDS)).isTrue();
		releaseLoaders.countDown();
		assertThat(first.get(1, TimeUnit.SECONDS).linkId()).isEqualTo(1L);
		assertThat(second.get(1, TimeUnit.SECONDS).linkId()).isEqualTo(2L);
	}

	@Test
	void reloadsOnlyAfterTtlExpires() {
		TestTicker ticker = new TestTicker();
		TestCache testCache = newCache(10L, ticker);
		AtomicInteger loads = new AtomicInteger();

		LinkAnalysisResult first = testCache.coordinator().getOrLoad(1L, () -> result(1L, loads.incrementAndGet()));
		ticker.advance(TTL.minusNanos(1));
		LinkAnalysisResult beforeExpiry = testCache.coordinator()
				.getOrLoad(1L, () -> result(1L, loads.incrementAndGet()));
		ticker.advance(Duration.ofNanos(1));
		LinkAnalysisResult afterExpiry = testCache.coordinator()
				.getOrLoad(1L, () -> result(1L, loads.incrementAndGet()));

		assertThat(beforeExpiry).isSameAs(first);
		assertThat(afterExpiry.contentText()).isEqualTo("load-2");
		assertThat(loads).hasValue(2);
	}

	@Test
	void doesNotCacheLoaderFailureAndRetriesNextCall() {
		TestCache testCache = newCache(10L);
		AtomicInteger loads = new AtomicInteger();

		assertThatThrownBy(() -> testCache.coordinator().getOrLoad(1L, () -> {
			loads.incrementAndGet();
			throw new IllegalStateException("temporary");
		})).isInstanceOf(IllegalStateException.class).hasMessage("temporary");

		LinkAnalysisResult retried = testCache.coordinator()
				.getOrLoad(1L, () -> result(1L, loads.incrementAndGet()));

		assertThat(retried.contentText()).isEqualTo("load-2");
		assertThat(loads).hasValue(2);
	}

	@Test
	void boundsWaitForAnExistingLoad() throws Exception {
		TestCache testCache = newCache(10L);
		LinkAnalysisCacheCoordinator coordinator = new LinkAnalysisCacheCoordinator(
				testCache.springCache(),
				Duration.ofMillis(25)
		);
		CountDownLatch loaderEntered = new CountDownLatch(1);
		CountDownLatch releaseLoader = new CountDownLatch(1);
		ExecutorService executor = newExecutor(1);
		Future<LinkAnalysisResult> owner = executor.submit(() -> coordinator.getOrLoad(1L, () -> {
			loaderEntered.countDown();
			await(releaseLoader);
			return result(1L, 1);
		}));
		assertThat(loaderEntered.await(1, TimeUnit.SECONDS)).isTrue();

		assertThatThrownBy(() -> coordinator.getOrLoad(1L, () -> result(1L, 2)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Timed out");
		releaseLoader.countDown();
		assertThat(owner.get(1, TimeUnit.SECONDS).contentText()).isEqualTo("load-1");
	}

	@Test
	void doesNotLeaveInFlightEntryIncompleteWhenLoaderThrowsError() {
		TestCache testCache = newCache(10L);
		AtomicInteger loads = new AtomicInteger();

		assertThatThrownBy(() -> testCache.coordinator().getOrLoad(1L, () -> {
			loads.incrementAndGet();
			throw new AssertionError("fatal");
		})).isInstanceOf(AssertionError.class).hasMessage("fatal");

		LinkAnalysisResult retried = testCache.coordinator()
				.getOrLoad(1L, () -> result(1L, loads.incrementAndGet()));
		assertThat(retried.contentText()).isEqualTo("load-2");
		assertThat(loads).hasValue(2);
	}

	@Test
	void sharesSingleFailureWithConcurrentWaiters() throws Exception {
		TestCache testCache = newCache(10L);
		AtomicInteger loads = new AtomicInteger();
		CountDownLatch loaderEntered = new CountDownLatch(1);
		CountDownLatch releaseLoader = new CountDownLatch(1);
		ExecutorService executor = newExecutor(CONCURRENT_CALLERS);

		List<Future<Throwable>> failures = new ArrayList<>();
		failures.add(executor.submit(() -> captureFailure(() -> testCache.coordinator().getOrLoad(1L, () -> {
			loads.incrementAndGet();
			loaderEntered.countDown();
			await(releaseLoader);
			throw new IllegalStateException("temporary");
		}))));
		assertThat(loaderEntered.await(1, TimeUnit.SECONDS)).isTrue();
		for (int caller = 1; caller < CONCURRENT_CALLERS; caller++) {
			failures.add(executor.submit(() -> captureFailure(() -> testCache.coordinator().getOrLoad(1L, () -> {
				loads.incrementAndGet();
				throw new IllegalStateException("temporary");
			}))));
		}
		assertThat(awaitWaiterCount(testCache.coordinator(), 1L, CONCURRENT_CALLERS - 1)).isTrue();
		releaseLoader.countDown();

		for (Future<Throwable> failure : failures) {
			assertThat(failure.get(1, TimeUnit.SECONDS))
					.isInstanceOf(IllegalStateException.class)
					.hasMessage("temporary");
		}
		assertThat(loads).hasValue(1);
	}

	@Test
	void keepsSizeWithinMaximumForHighCardinalityInput() {
		long maximumSize = 3L;
		TestCache testCache = newCache(maximumSize);

		for (long linkId = 1L; linkId <= 100L; linkId++) {
			long currentId = linkId;
			testCache.coordinator().getOrLoad(currentId, () -> result(currentId, 1));
		}
		testCache.nativeCache().cleanUp();

		assertThat(testCache.nativeCache().estimatedSize()).isLessThanOrEqualTo(maximumSize);
		assertThat(testCache.nativeCache().stats().evictionCount()).isPositive();
	}

	@Test
	void recordsCacheHitAndMissStats() {
		TestCache testCache = newCache(10L);

		testCache.coordinator().getOrLoad(1L, () -> result(1L, 1));
		testCache.coordinator().getOrLoad(1L, () -> result(1L, 2));

		assertThat(testCache.nativeCache().stats().missCount()).isEqualTo(2L);
		assertThat(testCache.nativeCache().stats().hitCount()).isEqualTo(1L);
	}

	private ExecutorService newExecutor(int threadCount) {
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		executors.add(executor);
		return executor;
	}

	private static LinkAnalysisResult loadAfterBarrier(
			LinkAnalysisCacheCoordinator coordinator,
			Long linkId,
			CountDownLatch entered,
			CountDownLatch release
	) {
		return coordinator.getOrLoad(linkId, () -> {
			entered.countDown();
			await(release);
			return result(linkId, 1);
		});
	}

	private static Throwable captureFailure(Runnable action) {
		try {
			action.run();
			throw new AssertionError("Expected action to fail");
		} catch (RuntimeException ex) {
			return ex;
		}
	}

	private static boolean awaitWaiterCount(
			LinkAnalysisCacheCoordinator coordinator,
			Long linkId,
			int expectedCount
	) throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1L);
		while (System.nanoTime() < deadline) {
			if (coordinator.waitingCallerCount(linkId) == expectedCount) {
				return true;
			}
			TimeUnit.MILLISECONDS.sleep(1L);
		}
		return false;
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(1, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Timed out while waiting for test coordination");
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for test coordination", ex);
		}
	}

	private static LinkAnalysisResult result(Long linkId, int loadNumber) {
		return new LinkAnalysisResult(linkId, LinkAnalysisStatus.SUCCEEDED, "load-" + loadNumber, null, null);
	}

	private static TestCache newCache(long maximumSize) {
		return newCache(maximumSize, Ticker.systemTicker());
	}

	private static TestCache newCache(long maximumSize, Ticker ticker) {
		com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = Caffeine.newBuilder()
				.maximumSize(maximumSize)
				.expireAfterWrite(TTL)
				.ticker(ticker)
				.recordStats()
				.build();
		CaffeineCache cache = new CaffeineCache(CacheNames.LINK_ANALYSIS_RESULTS, nativeCache);
		return new TestCache(new LinkAnalysisCacheCoordinator(cache), cache, nativeCache);
	}

	private record TestCache(
			LinkAnalysisCacheCoordinator coordinator,
			CaffeineCache springCache,
			com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache
	) {
	}

	private static final class StaleFirstReadCache extends ConcurrentMapCache {

		private final AtomicInteger reads = new AtomicInteger();
		private final CountDownLatch firstReadObserved = new CountDownLatch(1);
		private final CountDownLatch valueLoaded = new CountDownLatch(1);

		private StaleFirstReadCache() {
			super(CacheNames.LINK_ANALYSIS_RESULTS);
		}

		@Override
		public <T> T get(Object key, Class<T> type) {
			if (reads.incrementAndGet() == 1) {
				firstReadObserved.countDown();
				await(valueLoaded);
				return null;
			}
			return super.get(key, type);
		}

		@Override
		public void put(Object key, Object value) {
			super.put(key, value);
			valueLoaded.countDown();
		}
	}

	private static final class TestTicker implements Ticker {

		private final AtomicLong nanos = new AtomicLong();

		@Override
		public long read() {
			return nanos.get();
		}

		private void advance(Duration duration) {
			nanos.addAndGet(duration.toNanos());
		}
	}
}
