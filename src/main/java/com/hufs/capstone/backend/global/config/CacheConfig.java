package com.hufs.capstone.backend.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.hufs.capstone.backend.global.cache.CacheNames;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

	private static final long SINGLE_ENTRY_CACHE_SIZE = 1L;
	private static final long REGION_SIGUNGU_CACHE_SIZE = 32L;

	@Bean
	public CacheManager cacheManager(
			@Value("${app.cache.reference-data-ttl:24h}") Duration referenceDataTtl,
			@Value("${app.cache.link-analysis.ttl:2s}") Duration linkAnalysisTtl,
			@Value("${app.cache.link-analysis.maximum-size:10000}") long linkAnalysisMaximumSize
	) {
		validatePolicy(referenceDataTtl, "app.cache.reference-data-ttl");
		validatePolicy(linkAnalysisTtl, "app.cache.link-analysis.ttl");
		if (linkAnalysisMaximumSize <= 0) {
			throw new IllegalArgumentException("app.cache.link-analysis.maximum-size must be positive");
		}

		SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(List.of(
				newCache(CacheNames.PLACE_TAXONOMY_CATALOG, SINGLE_ENTRY_CACHE_SIZE, referenceDataTtl),
				newCache(CacheNames.PLACE_TAXONOMY_RESPONSE, SINGLE_ENTRY_CACHE_SIZE, referenceDataTtl),
				newCache(CacheNames.REGION_SIDOS, SINGLE_ENTRY_CACHE_SIZE, referenceDataTtl),
				newCache(CacheNames.REGION_SIGUNGUS, REGION_SIGUNGU_CACHE_SIZE, referenceDataTtl),
				newCache(CacheNames.REGION_ADDRESS_CATALOG, SINGLE_ENTRY_CACHE_SIZE, referenceDataTtl),
				newCache(CacheNames.LINK_ANALYSIS_RESULTS, linkAnalysisMaximumSize, linkAnalysisTtl)
		));
		return cacheManager;
	}

	private static CaffeineCache newCache(String name, long maximumSize, Duration ttl) {
		return new CaffeineCache(
				name,
				Caffeine.newBuilder()
						.maximumSize(maximumSize)
						.expireAfterWrite(ttl)
						.recordStats()
						.build()
		);
	}

	private static void validatePolicy(Duration duration, String propertyName) {
		if (duration == null || duration.isZero() || duration.isNegative()) {
			throw new IllegalArgumentException(propertyName + " must be positive");
		}
	}
}
