package com.hufs.capstone.backend.global.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.hufs.capstone.backend.place.application.PlaceTaxonomyReadService;
import com.hufs.capstone.backend.region.application.RegionAddressResolver;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest(properties = {
	"spring.jpa.properties.hibernate.generate_statistics=true",
	"logging.level.com.hufs.capstone.backend.global.cache.ReferenceDataQueryCountIntegrationTest=info"
})
@ActiveProfiles("test")
class ReferenceDataQueryCountIntegrationTest {

	private static final int REPEAT_COUNT = 5;
	private static final int CONCURRENT_CALLERS = 8;
	private static final long COLD_QUERY_COUNT = 2L;
	private static final long WARM_QUERY_COUNT = 0L;

	@Autowired
	private RegionAddressResolver regionAddressResolver;

	@Autowired
	private PlaceTaxonomyReadService placeTaxonomyReadService;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private CacheManager cacheManager;

	private Statistics statistics;

	@BeforeEach
	void setUp() {
		statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		cacheManager.getCacheNames().stream()
				.map(cacheManager::getCache)
				.filter(java.util.Objects::nonNull)
				.forEach(Cache::clear);
		statistics.clear();
	}

	@Test
	void loadsRegionAddressCatalogOnceAndExecutesNoWarmQueries() {
		regionAddressResolver.resolve("서울특별시 종로구 청운동", "서울특별시 종로구 자하문로 1");
		long coldQueryCount = statistics.getPrepareStatementCount();
		statistics.clear();

		for (int attempt = 0; attempt < REPEAT_COUNT; attempt++) {
			regionAddressResolver.resolve("서울특별시 종로구 청운동", "서울특별시 종로구 자하문로 1");
		}

		long warmQueryCount = statistics.getPrepareStatementCount();
		log.info("CACHE_QUERY_COUNT phase=after path=region-address cold_prepared_statements={} "
					+ "warm_repeats={} warm_prepared_statements={}",
				coldQueryCount, REPEAT_COUNT, warmQueryCount);

		assertThat(coldQueryCount).isEqualTo(COLD_QUERY_COUNT);
		assertThat(warmQueryCount).isEqualTo(WARM_QUERY_COUNT);
	}

	@Test
	void loadsPlaceTaxonomyCatalogOnceAndExecutesNoWarmQueries() {
		placeTaxonomyReadService.resolveTaxonomy("FD6", "음식점 > 일식 > 돈까스");
		long coldQueryCount = statistics.getPrepareStatementCount();
		statistics.clear();

		for (int attempt = 0; attempt < REPEAT_COUNT; attempt++) {
			placeTaxonomyReadService.resolveTaxonomy("FD6", "음식점 > 일식 > 돈까스");
		}

		long warmQueryCount = statistics.getPrepareStatementCount();
		log.info("CACHE_QUERY_COUNT phase=after path=place-taxonomy cold_prepared_statements={} "
					+ "warm_repeats={} warm_prepared_statements={}",
				coldQueryCount, REPEAT_COUNT, warmQueryCount);

		assertThat(coldQueryCount).isEqualTo(COLD_QUERY_COUNT);
		assertThat(warmQueryCount).isEqualTo(WARM_QUERY_COUNT);
	}

	@Test
	void collapsesConcurrentRegionCatalogLoadsIntoOneDatabaseLoad() throws Exception {
		long queryCount = runConcurrently(() -> regionAddressResolver.resolve(
				"서울특별시 종로구 청운동",
				"서울특별시 종로구 자하문로 1"
		));

		log.info("CACHE_QUERY_COUNT phase=after path=region-address concurrent_callers={} "
					+ "prepared_statements={}", CONCURRENT_CALLERS, queryCount);
		assertThat(queryCount).isEqualTo(COLD_QUERY_COUNT);
	}

	@Test
	void collapsesConcurrentTaxonomyCatalogLoadsIntoOneDatabaseLoad() throws Exception {
		long queryCount = runConcurrently(() -> placeTaxonomyReadService.resolveCategory(
				"FD6",
				"음식점 > 일식 > 돈까스"
		));

		log.info("CACHE_QUERY_COUNT phase=after path=place-taxonomy concurrent_callers={} "
					+ "prepared_statements={}", CONCURRENT_CALLERS, queryCount);
		assertThat(queryCount).isEqualTo(COLD_QUERY_COUNT);
	}

	private long runConcurrently(Runnable action) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_CALLERS);
		CountDownLatch ready = new CountDownLatch(CONCURRENT_CALLERS);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<?>> futures = new ArrayList<>();
			for (int caller = 0; caller < CONCURRENT_CALLERS; caller++) {
				futures.add(executor.submit(() -> {
					ready.countDown();
					await(start);
					action.run();
				}));
			}
			assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			for (Future<?> future : futures) {
				future.get(5, TimeUnit.SECONDS);
			}
			return statistics.getPrepareStatementCount();
		} finally {
			executor.shutdownNow();
		}
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(2, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Timed out while coordinating concurrent cache test");
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while coordinating concurrent cache test", exception);
		}
	}
}
