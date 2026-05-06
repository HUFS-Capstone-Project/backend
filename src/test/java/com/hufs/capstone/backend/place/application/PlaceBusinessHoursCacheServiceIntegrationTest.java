package com.hufs.capstone.backend.place.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursPlaceResponse;
import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursRequestStatus;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import com.hufs.capstone.backend.place.domain.repository.PlaceBusinessHoursRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
class PlaceBusinessHoursCacheServiceIntegrationTest {

	@Autowired
	private PlaceBusinessHoursCacheService cacheService;

	@Autowired
	private PlaceBusinessHoursRepository repository;

	@Autowired
	private ObjectMapper objectMapper;

	@AfterEach
	void tearDown() {
		repository.deleteAll();
	}

	@Test
	void shouldKeepBusinessHoursStatusWhenProcessingRequestFails() {
		cacheService.upsertRemotePlace(successPlace("job-1"), BusinessHoursRequestStatus.SUCCEEDED);

		cacheService.markRequestFailure(
				event(),
				BusinessHoursRequestStatus.REQUEST_FAILED,
				"connect failed"
		);

		PlaceBusinessHours cache = repository.findByKakaoPlaceId("13298463").orElseThrow();
		assertThat(cache.getBusinessHoursStatus()).isEqualTo(BusinessHoursStatus.SUCCESS);
		assertThat(cache.getLastRequestStatus()).isEqualTo(BusinessHoursRequestStatus.REQUEST_FAILED);
		assertThat(cache.getLastError()).isEqualTo("connect failed");
	}

	@Test
	void shouldUpsertSingleRowWhenSameKakaoPlaceIsProcessedConcurrently() throws Exception {
		runConcurrently(() -> {
			cacheService.upsertRemotePlace(pendingPlace("job-1"), BusinessHoursRequestStatus.SUCCEEDED);
			return null;
		}, 2);

		assertThat(repository.findByKakaoPlaceIdIn(List.of("13298463"))).hasSize(1);
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void shouldFindOnlyPendingOrFetchingRowsWithJobIdAfterPollingInterval() {
		cacheService.upsertRemotePlace(pendingPlace("job-1"), BusinessHoursRequestStatus.SUCCEEDED);

		List<PlaceBusinessHours> tooEarly = repository.findPollable(
				EnumSet.of(BusinessHoursStatus.PENDING, BusinessHoursStatus.FETCHING),
				Instant.now().minusSeconds(60),
				PageRequest.of(0, 10)
		);
		List<PlaceBusinessHours> due = repository.findPollable(
				EnumSet.of(BusinessHoursStatus.PENDING, BusinessHoursStatus.FETCHING),
				Instant.now().plusSeconds(1),
				PageRequest.of(0, 10)
		);

		assertThat(tooEarly).isEmpty();
		assertThat(due).hasSize(1);
		assertThat(due.get(0).getBusinessHoursJobId()).isEqualTo("job-1");
	}

	private BusinessHoursPlaceResponse successPlace(String jobId) {
		return new BusinessHoursPlaceResponse(
				"13298463",
				"https://place.map.kakao.com/13298463",
				"Test Place",
				objectMapper.createObjectNode().putArray("daily_hours"),
				"Thu 09:00 ~ 23:30",
				BusinessHoursStatus.SUCCESS,
				OffsetDateTime.parse("2026-05-07T10:00:03Z"),
				OffsetDateTime.parse("2026-05-21T10:00:03Z"),
				"kakao_place_crawl",
				jobId,
				null,
				null,
				null,
				1L
		);
	}

	private BusinessHoursPlaceResponse pendingPlace(String jobId) {
		return new BusinessHoursPlaceResponse(
				"13298463",
				"https://place.map.kakao.com/13298463",
				"Test Place",
				null,
				null,
				BusinessHoursStatus.PENDING,
				null,
				null,
				null,
				jobId,
				null,
				null,
				null,
				1L
		);
	}

	private static BusinessHoursRequestedEvent event() {
		return new BusinessHoursRequestedEvent(
				1L,
				1L,
				"13298463",
				"https://place.map.kakao.com/13298463",
				"Test Place",
				false,
				"test"
		);
	}

	private static <T> List<T> runConcurrently(Callable<T> task, int threadCount) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		try {
			CountDownLatch ready = new CountDownLatch(threadCount);
			CountDownLatch start = new CountDownLatch(1);
			List<Future<T>> futures = new java.util.ArrayList<>();
			for (int i = 0; i < threadCount; i++) {
				futures.add(executor.submit(() -> {
					ready.countDown();
					if (!start.await(3, TimeUnit.SECONDS)) {
						throw new IllegalStateException("start latch timeout");
					}
					return task.call();
				}));
			}
			if (!ready.await(3, TimeUnit.SECONDS)) {
				throw new IllegalStateException("ready latch timeout");
			}
			start.countDown();

			List<T> results = new java.util.ArrayList<>();
			for (Future<T> future : futures) {
				results.add(future.get(5, TimeUnit.SECONDS));
			}
			return results;
		} finally {
			executor.shutdownNow();
		}
	}
}
