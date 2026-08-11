package com.hufs.capstone.backend.place.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursPlaceResponse;
import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursRequestStatus;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import com.hufs.capstone.backend.place.domain.repository.PlaceBusinessHoursRepository;
import jakarta.persistence.OptimisticLockException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceBusinessHoursCacheService {

	private static final int MAX_UPSERT_ATTEMPTS = 3;

	private final PlaceBusinessHoursRepository placeBusinessHoursRepository;
	private final PlatformTransactionManager transactionManager;
	private final ObjectMapper objectMapper;
	private final BusinessHoursProperties businessHoursProperties;
	private final Clock clock;

	public void upsertRemotePlace(BusinessHoursPlaceResponse place, BusinessHoursRequestStatus requestStatus) {
		upsertRemotePlace(place, null, requestStatus);
	}

	public void upsertRemotePlace(
			BusinessHoursPlaceResponse place,
			String businessHoursJobId,
			BusinessHoursRequestStatus requestStatus
	) {
		if (place == null || isBlank(place.kakaoPlaceId())) {
			return;
		}
		String businessHoursJson = serializeBusinessHours(place);
		Instant expiresAt = remoteOrInProgressExpiry(place);
		retryUpsert(() -> upsertRemotePlaceOnce(
				place,
				businessHoursJson,
				expiresAt,
				businessHoursJobId,
				requestStatus
		));
	}

	public void markFailed(BusinessHoursRequestedEvent event, String lastError) {
		retryUpsert(() -> updateOnce(event.kakaoPlaceId(), cache -> cache.markFailed(
				event.placeUrl(),
				event.placeName(),
				lastError,
				clock.instant().plus(businessHoursProperties.detailRefresh().failureCooldown())
		), event.placeUrl(), event.placeName()));
	}

	public void markRequestFailure(
			BusinessHoursRequestedEvent event,
			BusinessHoursRequestStatus requestStatus,
			String lastError
	) {
		retryUpsert(() -> updateOnce(event.kakaoPlaceId(), cache -> cache.markRequestFailure(
				event.placeUrl(),
				event.placeName(),
				requestStatus,
				lastError
		), event.placeUrl(), event.placeName()));
	}

	public boolean claimPolling(
			Long cacheId,
			Collection<BusinessHoursStatus> statuses,
			Instant dueBefore,
			Instant claimedAt
	) {
		TransactionTemplate transactionTemplate = requiresNewTransactionTemplate();
		Integer updated = transactionTemplate.execute(status ->
				placeBusinessHoursRepository.claimPollable(cacheId, statuses, dueBefore, claimedAt));
		return updated != null && updated == 1;
	}

	private Instant remoteOrInProgressExpiry(BusinessHoursPlaceResponse place) {
		Instant remoteExpiresAt = toInstant(place.businessHoursExpiresAt());
		if (remoteExpiresAt != null || (place.businessHoursStatus() != BusinessHoursStatus.PENDING
				&& place.businessHoursStatus() != BusinessHoursStatus.FETCHING)) {
			return remoteExpiresAt;
		}
		return clock.instant().plus(businessHoursProperties.detailRefresh().jobTimeout());
	}

	private void upsertRemotePlaceOnce(
			BusinessHoursPlaceResponse place,
			String businessHoursJson,
			Instant effectiveExpiresAt,
			String businessHoursJobId,
			BusinessHoursRequestStatus requestStatus
	) {
		updateOnce(place.kakaoPlaceId(), cache -> cache.applyRemotePlace(
				place.placeUrl(),
				place.placeName(),
				businessHoursJson,
				null,
				place.businessHoursStatus(),
				toInstant(place.businessHoursFetchedAt()),
				effectiveExpiresAt,
				null,
				businessHoursJobId,
				errorDetails(place),
				requestStatus
		), place.placeUrl(), place.placeName());
	}

	private static String errorDetails(BusinessHoursPlaceResponse place) {
		if (!isBlank(place.errorCode()) && !isBlank(place.errorMessage())) {
			return place.errorCode() + ": " + place.errorMessage();
		}
		if (!isBlank(place.errorCode())) {
			return place.errorCode();
		}
		if (!isBlank(place.errorMessage())) {
			return place.errorMessage();
		}
		return null;
	}

	private void updateOnce(
			String kakaoPlaceId,
			CacheMutator mutator,
			String placeUrl,
			String placeName
	) {
		TransactionTemplate transactionTemplate = requiresNewTransactionTemplate();
		transactionTemplate.executeWithoutResult(status -> {
			PlaceBusinessHours cache = placeBusinessHoursRepository.findByKakaoPlaceId(kakaoPlaceId)
					.orElseGet(() -> PlaceBusinessHours.create(kakaoPlaceId, placeUrl, placeName));
			mutator.mutate(cache);
			placeBusinessHoursRepository.saveAndFlush(cache);
		});
	}

	private void retryUpsert(Runnable upsert) {
		RuntimeException lastException = null;
		for (int attempt = 1; attempt <= MAX_UPSERT_ATTEMPTS; attempt++) {
			try {
				upsert.run();
				return;
			} catch (DataIntegrityViolationException
					| ObjectOptimisticLockingFailureException
					| OptimisticLockException ex) {
				lastException = ex;
				log.debug("Business hours cache upsert retry. attempt={}/{}", attempt, MAX_UPSERT_ATTEMPTS, ex);
			}
		}
		throw lastException == null ? new IllegalStateException("Business hours cache upsert failed.") : lastException;
	}

	private TransactionTemplate requiresNewTransactionTemplate() {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return transactionTemplate;
	}

	private String serializeBusinessHours(BusinessHoursPlaceResponse place) {
		if (place.businessHours() == null || place.businessHours().isNull()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(place.businessHours());
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Business hours JSON serialization failed.", ex);
		}
	}

	private static Instant toInstant(OffsetDateTime dateTime) {
		return dateTime == null ? null : dateTime.toInstant();
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	@FunctionalInterface
	private interface CacheMutator {

		void mutate(PlaceBusinessHours cache);
	}
}
