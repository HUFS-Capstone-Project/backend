package com.hufs.capstone.backend.global.cache;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class ReferenceDataCacheInvalidator {

	private final CacheManager cacheManager;

	public void clearRegionCaches() {
		clear(CacheNames.REGION_CACHES);
	}

	public void clearPlaceTaxonomyCaches() {
		clear(CacheNames.PLACE_TAXONOMY_CACHES);
	}

	public void clearPlaceTaxonomyCachesAfterCommit() {
		if (!TransactionSynchronizationManager.isSynchronizationActive()
				|| !TransactionSynchronizationManager.isActualTransactionActive()) {
			throw new IllegalStateException("Cache eviction after commit requires an active transaction");
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				clearPlaceTaxonomyCaches();
			}
		});
	}

	private void clear(Collection<String> cacheNames) {
		for (String cacheName : cacheNames) {
			requireCache(cacheName).clear();
		}
	}

	private Cache requireCache(String cacheName) {
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			throw new IllegalStateException("Missing cache: " + cacheName);
		}
		return cache;
	}
}
