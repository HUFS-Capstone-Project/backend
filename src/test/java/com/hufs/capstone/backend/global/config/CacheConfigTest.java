package com.hufs.capstone.backend.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.benmanes.caffeine.cache.Cache;
import com.hufs.capstone.backend.global.cache.CacheNames;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;

class CacheConfigTest {

	private static final Duration REFERENCE_DATA_TTL = Duration.ofMinutes(7);
	private static final Duration LINK_ANALYSIS_TTL = Duration.ofSeconds(3);

	@Test
	void createsAllExpectedCachesAndNoOthers() {
		SimpleCacheManager cacheManager = configuredCacheManager(REFERENCE_DATA_TTL, LINK_ANALYSIS_TTL, 10L);

		assertThat(cacheManager.getCacheNames()).containsExactlyInAnyOrder(
				CacheNames.PLACE_TAXONOMY_CATALOG,
				CacheNames.PLACE_TAXONOMY_RESPONSE,
				CacheNames.REGION_SIDOS,
				CacheNames.REGION_SIGUNGUS,
				CacheNames.REGION_ADDRESS_CATALOG,
				CacheNames.LINK_ANALYSIS_RESULTS
		);
	}

	@Test
	void appliesReferenceDataTtlAndBoundedSizes() {
		SimpleCacheManager cacheManager = configuredCacheManager(REFERENCE_DATA_TTL, LINK_ANALYSIS_TTL, 10L);
		Map<String, Long> expectedMaximumSizes = Map.of(
				CacheNames.PLACE_TAXONOMY_CATALOG, 1L,
				CacheNames.PLACE_TAXONOMY_RESPONSE, 1L,
				CacheNames.REGION_SIDOS, 1L,
				CacheNames.REGION_SIGUNGUS, 32L,
				CacheNames.REGION_ADDRESS_CATALOG, 1L
		);

		expectedMaximumSizes.forEach((cacheName, maximumSize) -> {
			Cache<Object, Object> cache = nativeCache(cacheManager, cacheName);

			assertThat(expireAfterWriteNanos(cache)).isEqualTo(REFERENCE_DATA_TTL.toNanos());
			assertThat(maximumSize(cache)).isEqualTo(maximumSize);

			putMoreThanMaximum(cache, maximumSize);
			assertThat(cache.estimatedSize()).isLessThanOrEqualTo(maximumSize);
			assertThat(cache.stats().evictionCount()).isPositive();
		});
	}

	@Test
	void appliesLinkAnalysisTtlAndConfiguredMaximumSize() {
		long configuredMaximumSize = 3L;
		SimpleCacheManager cacheManager = configuredCacheManager(
				REFERENCE_DATA_TTL,
				LINK_ANALYSIS_TTL,
				configuredMaximumSize
		);
		Cache<Object, Object> cache = nativeCache(cacheManager, CacheNames.LINK_ANALYSIS_RESULTS);

		assertThat(expireAfterWriteNanos(cache)).isEqualTo(LINK_ANALYSIS_TTL.toNanos());
		assertThat(maximumSize(cache)).isEqualTo(configuredMaximumSize);

		putMoreThanMaximum(cache, configuredMaximumSize);
		assertThat(cache.estimatedSize()).isLessThanOrEqualTo(configuredMaximumSize);
		assertThat(cache.stats().evictionCount()).isPositive();
	}

	@Test
	void rejectsMissingOrNonPositiveReferenceDataTtl() {
		assertInvalidPolicy(null, LINK_ANALYSIS_TTL, 10L, "app.cache.reference-data-ttl");
		assertInvalidPolicy(Duration.ZERO, LINK_ANALYSIS_TTL, 10L, "app.cache.reference-data-ttl");
		assertInvalidPolicy(Duration.ofNanos(-1L), LINK_ANALYSIS_TTL, 10L, "app.cache.reference-data-ttl");
	}

	@Test
	void rejectsMissingOrNonPositiveLinkAnalysisTtl() {
		assertInvalidPolicy(REFERENCE_DATA_TTL, null, 10L, "app.cache.link-analysis.ttl");
		assertInvalidPolicy(REFERENCE_DATA_TTL, Duration.ZERO, 10L, "app.cache.link-analysis.ttl");
		assertInvalidPolicy(REFERENCE_DATA_TTL, Duration.ofNanos(-1L), 10L, "app.cache.link-analysis.ttl");
	}

	@Test
	void rejectsNonPositiveLinkAnalysisMaximumSize() {
		assertInvalidPolicy(REFERENCE_DATA_TTL, LINK_ANALYSIS_TTL, 0L, "app.cache.link-analysis.maximum-size");
		assertInvalidPolicy(REFERENCE_DATA_TTL, LINK_ANALYSIS_TTL, -1L, "app.cache.link-analysis.maximum-size");
	}

	private static SimpleCacheManager configuredCacheManager(
			Duration referenceDataTtl,
			Duration linkAnalysisTtl,
			long linkAnalysisMaximumSize
	) {
		SimpleCacheManager cacheManager = (SimpleCacheManager) new CacheConfig()
				.cacheManager(referenceDataTtl, linkAnalysisTtl, linkAnalysisMaximumSize);
		cacheManager.initializeCaches();
		return cacheManager;
	}

	private static Cache<Object, Object> nativeCache(SimpleCacheManager cacheManager, String cacheName) {
		org.springframework.cache.Cache springCache = cacheManager.getCache(cacheName);
		assertThat(springCache).isInstanceOf(CaffeineCache.class);
		return ((CaffeineCache) springCache).getNativeCache();
	}

	private static long expireAfterWriteNanos(Cache<Object, Object> cache) {
		return cache.policy()
				.expireAfterWrite()
				.orElseThrow()
				.getExpiresAfter(TimeUnit.NANOSECONDS);
	}

	private static long maximumSize(Cache<Object, Object> cache) {
		return cache.policy().eviction().orElseThrow().getMaximum();
	}

	private static void putMoreThanMaximum(Cache<Object, Object> cache, long maximumSize) {
		for (long key = 0L; key < maximumSize + 10L; key++) {
			cache.put(key, "value-" + key);
		}
		cache.cleanUp();
	}

	private static void assertInvalidPolicy(
			Duration referenceDataTtl,
			Duration linkAnalysisTtl,
			long linkAnalysisMaximumSize,
			String propertyName
	) {
		assertThatThrownBy(() -> new CacheConfig().cacheManager(
				referenceDataTtl,
				linkAnalysisTtl,
				linkAnalysisMaximumSize
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(propertyName)
				.hasMessageContaining("must be positive");
	}
}
