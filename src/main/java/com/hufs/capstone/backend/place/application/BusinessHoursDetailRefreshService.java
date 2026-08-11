package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.repository.PlaceBusinessHoursRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessHoursDetailRefreshService {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
	private static final int MAX_CLAIM_ATTEMPTS = 2;

	private final PlaceBusinessHoursRepository placeBusinessHoursRepository;
	private final PlaceBusinessHoursRefreshPolicy refreshPolicy;
	private final BusinessHoursProperties properties;
	private final ApplicationEventPublisher eventPublisher;
	private final PlatformTransactionManager transactionManager;
	private final Clock clock;

	public void requestIfNeeded(RoomPlace roomPlace) {
		Place place = roomPlace.getPlace();
		BusinessHoursRequestSnapshot snapshot = new BusinessHoursRequestSnapshot(
				roomPlace.getId(),
				place.getId(),
				place.getKakaoPlaceId(),
				place.getPlaceUrl(),
				place.getName()
		);
		try {
			for (int attempt = 1; attempt <= MAX_CLAIM_ATTEMPTS; attempt++) {
				try {
					requestInNewTransaction(snapshot);
					return;
				} catch (DataIntegrityViolationException ex) {
					if (attempt == MAX_CLAIM_ATTEMPTS) {
						throw ex;
					}
				}
			}
		} catch (RuntimeException ex) {
			log.warn(
					"Business hours detail refresh claim failed. roomPlaceId={}, kakaoPlaceId={}",
					snapshot.roomPlaceId(),
					snapshot.kakaoPlaceId(),
					ex
			);
		}
	}

	private void requestInNewTransaction(BusinessHoursRequestSnapshot snapshot) {
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);
		transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transaction.executeWithoutResult(status -> {
			Instant now = clock.instant();
			LocalDate requiredDate = now.atZone(SEOUL_ZONE).toLocalDate();
			PlaceBusinessHours cache = placeBusinessHoursRepository
					.findByKakaoPlaceIdForUpdate(snapshot.kakaoPlaceId())
					.orElse(null);
			if (!refreshPolicy.shouldRequestForDetail(cache, requiredDate, now, properties.detailRefresh())) {
				return;
			}
			if (cache == null) {
				cache = PlaceBusinessHours.create(snapshot.kakaoPlaceId(), snapshot.placeUrl(), snapshot.placeName());
			}
			cache.markRefreshRequested(
					snapshot.placeUrl(),
					snapshot.placeName(),
					now,
					now.plus(properties.detailRefresh().jobTimeout())
			);
			placeBusinessHoursRepository.saveAndFlush(cache);
			eventPublisher.publishEvent(new BusinessHoursRequestedEvent(
					snapshot.roomPlaceId(),
					snapshot.placeId(),
					snapshot.kakaoPlaceId(),
					snapshot.placeUrl(),
					snapshot.placeName(),
					requiredDate,
					false,
					"detail_daily_refresh"
			));
		});
	}

	private record BusinessHoursRequestSnapshot(
			Long roomPlaceId,
			Long placeId,
			String kakaoPlaceId,
			String placeUrl,
			String placeName
	) {
	}
}
