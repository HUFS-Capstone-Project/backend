package com.hufs.capstone.backend.place.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursPlaceResponse;
import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursRequestStatus;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import com.hufs.capstone.backend.place.domain.repository.PlaceBusinessHoursRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@Timeout(30)
class PlaceBusinessHoursCacheServiceIntegrationTest {

	@Autowired
	private PlaceBusinessHoursCacheService cacheService;

	@Autowired
	private PlaceBusinessHoursRepository repository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void tearDown() {
		repository.deleteAll();
	}

	@Test
	void shouldKeepBusinessHoursStatusWhenProcessingRequestFails() {
		cacheService.upsertRemotePlace(successPlace(), "job-1", BusinessHoursRequestStatus.SUCCEEDED);

		cacheService.markRequestFailure(
				event(),
				BusinessHoursRequestStatus.FAILED,
				"connect failed"
		);

		PlaceBusinessHours cache = repository.findByKakaoPlaceId("13298463").orElseThrow();
		assertThat(cache.getBusinessHoursStatus()).isEqualTo(BusinessHoursStatus.SUCCEEDED);
		assertThat(cache.getLastRequestStatus()).isEqualTo(BusinessHoursRequestStatus.FAILED);
		assertThat(cache.getLastError()).isEqualTo("connect failed");
	}

	@Test
	void shouldPreserveRemoteExpiryBecauseDailyFreshnessUsesFetchedDate() {
		cacheService.upsertRemotePlace(successPlace(), "job-1", BusinessHoursRequestStatus.SUCCEEDED);

		PlaceBusinessHours cache = repository.findByKakaoPlaceId("13298463").orElseThrow();
		assertThat(cache.getBusinessHoursExpiresAt()).isEqualTo(Instant.parse("2026-05-21T10:00:03Z"));
	}

	@Test
	void shouldPreserveLastSuccessfulPayloadWhileRefreshIsPending() {
		cacheService.upsertRemotePlace(successPlace(), "job-1", BusinessHoursRequestStatus.SUCCEEDED);
		String successfulJson = repository.findByKakaoPlaceId("13298463").orElseThrow().getBusinessHoursJson();

		Instant requestedAfter = Instant.now();
		cacheService.upsertRemotePlace(pendingPlace(), "job-2", BusinessHoursRequestStatus.SUCCEEDED);

		PlaceBusinessHours cache = repository.findByKakaoPlaceId("13298463").orElseThrow();
		assertThat(cache.getBusinessHoursStatus()).isEqualTo(BusinessHoursStatus.PENDING);
		assertThat(cache.getBusinessHoursJson()).isEqualTo(successfulJson);
		assertThat(cache.getBusinessHoursFetchedAt()).isEqualTo(Instant.parse("2026-05-07T10:00:03Z"));
		assertThat(cache.getBusinessHoursExpiresAt()).isAfterOrEqualTo(requestedAfter.plusSeconds(14 * 60));
		assertThat(cache.getBusinessHoursExpiresAt()).isBeforeOrEqualTo(Instant.now().plusSeconds(16 * 60));
	}

	@Test
	void shouldUpsertSingleRowWhenSameKakaoPlaceIsProcessedConcurrently() throws Exception {
		runConcurrently(() -> {
			cacheService.upsertRemotePlace(pendingPlace(), "job-1", BusinessHoursRequestStatus.SUCCEEDED);
			return null;
		}, 2);

		assertThat(repository.findByKakaoPlaceIdIn(List.of("13298463"))).hasSize(1);
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void shouldFindOnlyPendingOrFetchingRowsWithJobIdAfterPollingInterval() {
		cacheService.upsertRemotePlace(pendingPlace(), "job-1", BusinessHoursRequestStatus.SUCCEEDED);

		List<PlaceBusinessHours> tooEarly = repository.findPollable(
				Instant.now().minusSeconds(60),
				PageRequest.of(0, 10)
		);
		List<PlaceBusinessHours> due = repository.findPollable(
				Instant.now().plusSeconds(1),
				PageRequest.of(0, 10)
		);

		assertThat(tooEarly).isEmpty();
		assertThat(due).hasSize(1);
		assertThat(due.get(0).getBusinessHoursJobId()).isEqualTo("job-1");
	}

	@Test
	void shouldPrioritizeTheRowWithTheOldestEffectivePollTime() {
		Instant now = Instant.parse("2026-08-16T12:00:00Z");
		cacheService.upsertRemotePlace(pendingPlace("recently-polled"), "job-recent", BusinessHoursRequestStatus.SUCCEEDED);
		cacheService.upsertRemotePlace(pendingPlace("most-overdue"), "job-overdue", BusinessHoursRequestStatus.SUCCEEDED);

		updatePollingTimes("recently-polled", now.minusSeconds(7_200), now.minusSeconds(300));
		updatePollingTimes("most-overdue", now.minusSeconds(3_600), now.minusSeconds(1_800));

		List<PlaceBusinessHours> result = repository.findPollable(
				now.minusSeconds(60),
				PageRequest.of(0, 1)
		);

		assertThat(result).extracting(PlaceBusinessHours::getKakaoPlaceId).containsExactly("most-overdue");
	}

	@Test
	void shouldAllowOnlyOneClaimWithinTheSamePollingWindow() {
		Instant now = Instant.parse("2026-08-16T12:00:00Z");
		cacheService.upsertRemotePlace(pendingPlace("claim-once"), "job-claim", BusinessHoursRequestStatus.SUCCEEDED);
		updatePollingTimes("claim-once", now.minusSeconds(600), now.minusSeconds(600));
		Long cacheId = repository.findByKakaoPlaceId("claim-once").orElseThrow().getId();

		boolean firstClaim = cacheService.claimPolling(cacheId, now.minusSeconds(60), now);
		boolean duplicateClaim = cacheService.claimPolling(cacheId, now.minusSeconds(60), now.plusSeconds(1));

		assertThat(firstClaim).isTrue();
		assertThat(duplicateClaim).isFalse();
	}

	private BusinessHoursPlaceResponse successPlace() {
		return new BusinessHoursPlaceResponse(
				"13298463",
				"Test Place",
				"https://place.map.kakao.com/13298463",
				BusinessHoursStatus.SUCCEEDED,
				objectMapper.createObjectNode().putArray("daily_hours"),
				OffsetDateTime.parse("2026-05-07T10:00:03Z"),
				OffsetDateTime.parse("2026-05-21T10:00:03Z"),
				null,
				null
		);
	}

	private BusinessHoursPlaceResponse pendingPlace() {
		return pendingPlace("13298463");
	}

	private BusinessHoursPlaceResponse pendingPlace(String kakaoPlaceId) {
		return new BusinessHoursPlaceResponse(
				kakaoPlaceId,
				"Test Place",
				"https://place.map.kakao.com/" + kakaoPlaceId,
				BusinessHoursStatus.PENDING,
				null,
				null,
				null,
				null,
				null
		);
	}

	private void updatePollingTimes(String kakaoPlaceId, Instant updatedAt, Instant lastPolledAt) {
		jdbcTemplate.update(
				"""
				update place_business_hours
				set updated_at = ?, last_polled_at = ?
				where kakao_place_id = ?
				""",
				Timestamp.from(updatedAt),
				Timestamp.from(lastPolledAt),
				kakaoPlaceId
		);
	}

	private static BusinessHoursRequestedEvent event() {
		return new BusinessHoursRequestedEvent(
				1L,
				1L,
				"13298463",
				"https://place.map.kakao.com/13298463",
				"Test Place",
				java.time.LocalDate.of(2026, 5, 7),
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
