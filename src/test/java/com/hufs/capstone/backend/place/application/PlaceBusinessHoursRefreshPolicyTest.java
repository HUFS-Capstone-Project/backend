package com.hufs.capstone.backend.place.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursRequestStatus;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlaceBusinessHoursRefreshPolicyTest {

	private final PlaceBusinessHoursRefreshPolicy policy = new PlaceBusinessHoursRefreshPolicy();

	@Test
	void shouldRequestWhenCacheIsMissing() {
		assertThat(policy.shouldRequest(null, Instant.parse("2026-05-07T00:00:00Z"))).isTrue();
	}

	@Test
	void shouldTreatNullExpiresAtAsExpired() {
		PlaceBusinessHours cache = cache(BusinessHoursStatus.SUCCEEDED, null, null);

		assertThat(policy.shouldRequest(cache, Instant.parse("2026-05-07T00:00:00Z"))).isTrue();
	}

	@Test
	void shouldNotRequestWhenPendingOrFetchingHasJobId() {
		Instant now = Instant.parse("2026-05-07T00:00:00Z");

		assertThat(policy.shouldRequest(cache(BusinessHoursStatus.PENDING, "job-1", null), now)).isFalse();
		assertThat(policy.shouldRequest(cache(BusinessHoursStatus.FETCHING, "job-2", null), now)).isFalse();
	}

	@Test
	void shouldRequestOnlyWhenExpiresAtIsNotFuture() {
		Instant now = Instant.parse("2026-05-07T00:00:00Z");

		assertThat(policy.shouldRequest(
				cache(BusinessHoursStatus.SUCCEEDED, "job-1", Instant.parse("2026-05-08T00:00:00Z")),
				now
		)).isFalse();
		assertThat(policy.shouldRequest(
				cache(BusinessHoursStatus.SUCCEEDED, "job-1", Instant.parse("2026-05-07T00:00:00Z")),
				now
		)).isTrue();
	}

	private static PlaceBusinessHours cache(BusinessHoursStatus status, String jobId, Instant expiresAt) {
		PlaceBusinessHours cache = PlaceBusinessHours.create(
				"13298463",
				"https://place.map.kakao.com/13298463",
				"Test Place"
		);
		cache.applyRemotePlace(
				"https://place.map.kakao.com/13298463",
				"Test Place",
				null,
				null,
				status,
				null,
				expiresAt,
				"kakao_place_crawl",
				jobId,
				null,
				BusinessHoursRequestStatus.SUCCEEDED
		);
		return cache;
	}
}
