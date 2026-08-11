package com.hufs.capstone.backend.place.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursRequestStatus;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PlaceBusinessHoursRefreshPolicyTest {

	private static final BusinessHoursProperties.DetailRefresh SETTINGS =
			new BusinessHoursProperties.DetailRefresh(
					Duration.ofMinutes(2), Duration.ofMinutes(15), Duration.ofHours(1));
	private static final LocalDate REQUIRED_DATE = LocalDate.of(2026, 5, 7);
	private static final Instant NOW = Instant.parse("2026-05-07T03:00:00Z");

	private final PlaceBusinessHoursRefreshPolicy policy = new PlaceBusinessHoursRefreshPolicy();

	@Test
	void shouldRequestMissingCacheInitiallyAndOnDetail() {
		assertThat(policy.shouldRequestInitially(null)).isTrue();
		assertThat(policy.shouldRequestForDetail(null, REQUIRED_DATE, NOW, SETTINGS)).isTrue();
	}

	@Test
	void shouldNotRefreshSuccessfulCacheTwiceOnSameSeoulDate() {
		PlaceBusinessHours cache = succeededAt(Instant.parse("2026-05-06T16:00:00Z"));

		assertThat(policy.shouldRequestForDetail(cache, REQUIRED_DATE, NOW, SETTINGS)).isFalse();
	}

	@Test
	void shouldRefreshSuccessfulCacheOnNextSeoulDateEvenBeforeRemoteExpiry() {
		PlaceBusinessHours cache = succeededAt(Instant.parse("2026-05-06T14:59:59Z"));

		assertThat(policy.shouldRequestForDetail(cache, REQUIRED_DATE, NOW, SETTINGS)).isTrue();
	}

	@Test
	void shouldNotDuplicateAnActiveRemoteJob() {
		PlaceBusinessHours cache = cache(BusinessHoursStatus.PENDING, "job-1", null, null);

		assertThat(policy.shouldRequestForDetail(cache, REQUIRED_DATE, NOW, SETTINGS)).isFalse();
	}

	@Test
	void shouldRecoverAnActiveRemoteJobAfterItsBoundedTimeout() {
		PlaceBusinessHours cache = cache(BusinessHoursStatus.PENDING, "job-1", null, NOW.minusSeconds(1));

		assertThat(policy.shouldRequestForDetail(cache, REQUIRED_DATE, NOW, SETTINGS)).isTrue();
	}

	@Test
	void shouldRetryOnlyAfterAnUndispatchedRequestTimesOut() {
		PlaceBusinessHours cache = PlaceBusinessHours.create(
				"13298463", "https://place.map.kakao.com/13298463", "Test Place");
		cache.markRefreshRequested(
				cache.getPlaceUrl(), cache.getPlaceName(), NOW.minusSeconds(60), NOW.plusSeconds(840));
		assertThat(policy.shouldRequestForDetail(cache, REQUIRED_DATE, NOW, SETTINGS)).isFalse();

		cache.markRefreshRequested(
				cache.getPlaceUrl(), cache.getPlaceName(), NOW.minusSeconds(121), NOW.plusSeconds(779));
		assertThat(policy.shouldRequestForDetail(cache, REQUIRED_DATE, NOW, SETTINGS)).isTrue();
	}

	@Test
	void shouldRespectFailureCooldownWithoutManualRetryUi() {
		PlaceBusinessHours cache = PlaceBusinessHours.create(
				"13298463", "https://place.map.kakao.com/13298463", "Test Place");
		cache.markFailed(cache.getPlaceUrl(), cache.getPlaceName(), "temporary", NOW.plusSeconds(3600));

		assertThat(policy.shouldRequestForDetail(cache, REQUIRED_DATE, NOW, SETTINGS)).isFalse();
		assertThat(policy.shouldRequestForDetail(cache, REQUIRED_DATE, NOW.plusSeconds(3601), SETTINGS)).isTrue();
	}

	private static PlaceBusinessHours succeededAt(Instant fetchedAt) {
		return cache(BusinessHoursStatus.SUCCEEDED, "job-1", fetchedAt, NOW.plusSeconds(86400));
	}

	private static PlaceBusinessHours cache(
			BusinessHoursStatus status,
			String jobId,
			Instant fetchedAt,
			Instant expiresAt
	) {
		PlaceBusinessHours cache = PlaceBusinessHours.create(
				"13298463", "https://place.map.kakao.com/13298463", "Test Place");
		cache.applyRemotePlace(
				cache.getPlaceUrl(), cache.getPlaceName(), fetchedAt == null ? null : "{}", null,
				status, fetchedAt, expiresAt, "kakao_place_crawl", jobId, null,
				BusinessHoursRequestStatus.SUCCEEDED
		);
		return cache;
	}
}
