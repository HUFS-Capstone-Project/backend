package com.hufs.capstone.backend.global.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class ReferenceDataCacheInvalidatorTest {

	@Test
	void clearsOnlyRegionCaches() {
		Map<String, ConcurrentMapCache> caches = allCaches();
		ReferenceDataCacheInvalidator invalidator = new ReferenceDataCacheInvalidator(cacheManager(caches));
		caches.values().forEach(cache -> cache.put("key", "value"));

		invalidator.clearRegionCaches();

		for (String cacheName : CacheNames.REGION_CACHES) {
			assertThat(caches.get(cacheName).get("key")).isNull();
		}
		assertThat(caches.get(CacheNames.PLACE_TAXONOMY_CATALOG).get("key")).isNotNull();
		assertThat(caches.get(CacheNames.LINK_ANALYSIS_RESULTS).get("key")).isNotNull();
	}

	@Test
	void clearsOnlyPlaceTaxonomyCaches() {
		Map<String, ConcurrentMapCache> caches = allCaches();
		ReferenceDataCacheInvalidator invalidator = new ReferenceDataCacheInvalidator(cacheManager(caches));
		caches.values().forEach(cache -> cache.put("key", "value"));

		invalidator.clearPlaceTaxonomyCaches();

		for (String cacheName : CacheNames.PLACE_TAXONOMY_CACHES) {
			assertThat(caches.get(cacheName).get("key")).isNull();
		}
		assertThat(caches.get(CacheNames.REGION_ADDRESS_CATALOG).get("key")).isNotNull();
		assertThat(caches.get(CacheNames.LINK_ANALYSIS_RESULTS).get("key")).isNotNull();
	}

	@Test
	void keepsTaxonomyCachesUntilCommitAndClearsThemAfterCommit() {
		Map<String, ConcurrentMapCache> caches = allCaches();
		ReferenceDataCacheInvalidator invalidator = new ReferenceDataCacheInvalidator(cacheManager(caches));
		Cache catalog = caches.get(CacheNames.PLACE_TAXONOMY_CATALOG);
		catalog.put("key", "old-value");

		new TransactionTemplate(new TestTransactionManager()).executeWithoutResult(status -> {
			invalidator.clearPlaceTaxonomyCachesAfterCommit();
			assertThat(catalog.get("key", String.class)).isEqualTo("old-value");
		});

		assertThat(catalog.get("key")).isNull();
	}

	@Test
	void keepsTaxonomyCachesWhenTransactionRollsBack() {
		Map<String, ConcurrentMapCache> caches = allCaches();
		ReferenceDataCacheInvalidator invalidator = new ReferenceDataCacheInvalidator(cacheManager(caches));
		Cache catalog = caches.get(CacheNames.PLACE_TAXONOMY_CATALOG);
		catalog.put("key", "old-value");

		new TransactionTemplate(new TestTransactionManager()).executeWithoutResult(status -> {
			invalidator.clearPlaceTaxonomyCachesAfterCommit();
			status.setRollbackOnly();
		});

		assertThat(catalog.get("key", String.class)).isEqualTo("old-value");
	}

	@Test
	void rejectsAfterCommitEvictionWithoutTransactionSynchronization() {
		ReferenceDataCacheInvalidator invalidator = new ReferenceDataCacheInvalidator(cacheManager(allCaches()));

		assertThatThrownBy(invalidator::clearPlaceTaxonomyCachesAfterCommit)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("active transaction");
	}

	@Test
	void rejectsAfterCommitEvictionWithSynchronizationButWithoutActualTransaction() {
		ReferenceDataCacheInvalidator invalidator = new ReferenceDataCacheInvalidator(cacheManager(allCaches()));

		TransactionSynchronizationManager.initSynchronization();
		try {
			assertThatThrownBy(invalidator::clearPlaceTaxonomyCachesAfterCommit)
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("active transaction");
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void failsFastWhenARequiredCacheIsMissing() {
		SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(java.util.List.of());
		cacheManager.initializeCaches();
		ReferenceDataCacheInvalidator invalidator = new ReferenceDataCacheInvalidator(cacheManager);

		assertThatThrownBy(invalidator::clearRegionCaches)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(CacheNames.REGION_SIDOS);
	}

	private static Map<String, ConcurrentMapCache> allCaches() {
		Map<String, ConcurrentMapCache> caches = new LinkedHashMap<>();
		caches.put(CacheNames.PLACE_TAXONOMY_CATALOG, new ConcurrentMapCache(CacheNames.PLACE_TAXONOMY_CATALOG));
		caches.put(CacheNames.PLACE_TAXONOMY_RESPONSE, new ConcurrentMapCache(CacheNames.PLACE_TAXONOMY_RESPONSE));
		caches.put(CacheNames.REGION_SIDOS, new ConcurrentMapCache(CacheNames.REGION_SIDOS));
		caches.put(CacheNames.REGION_SIGUNGUS, new ConcurrentMapCache(CacheNames.REGION_SIGUNGUS));
		caches.put(CacheNames.REGION_ADDRESS_CATALOG, new ConcurrentMapCache(CacheNames.REGION_ADDRESS_CATALOG));
		caches.put(CacheNames.LINK_ANALYSIS_RESULTS, new ConcurrentMapCache(CacheNames.LINK_ANALYSIS_RESULTS));
		return caches;
	}

	private static SimpleCacheManager cacheManager(Map<String, ? extends Cache> caches) {
		SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(caches.values());
		cacheManager.initializeCaches();
		return cacheManager;
	}

	private static final class TestTransactionManager extends AbstractPlatformTransactionManager {

		@Override
		protected Object doGetTransaction() {
			return new Object();
		}

		@Override
		protected void doBegin(Object transaction, TransactionDefinition definition) {
			// No resource is needed; AbstractPlatformTransactionManager drives synchronization callbacks.
		}

		@Override
		protected void doCommit(DefaultTransactionStatus status) {
			// No resource is needed for this transaction synchronization test.
		}

		@Override
		protected void doRollback(DefaultTransactionStatus status) {
			// No resource is needed for this transaction synchronization test.
		}
	}
}
