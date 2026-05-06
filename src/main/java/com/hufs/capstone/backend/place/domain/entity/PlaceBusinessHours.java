package com.hufs.capstone.backend.place.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursRequestStatus;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "place_business_hours",
		indexes = {
			@Index(name = "idx_place_business_hours_status_job", columnList = "business_hours_status, business_hours_job_id"),
			@Index(name = "idx_place_business_hours_polling", columnList = "business_hours_status, business_hours_job_id, last_polled_at"),
			@Index(name = "idx_place_business_hours_expires_at", columnList = "business_hours_expires_at")
		},
		uniqueConstraints = {
			@UniqueConstraint(
					name = "uq_place_business_hours_kakao_place_id",
					columnNames = "kakao_place_id"
				)
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceBusinessHours extends AuditableEntity {

	@Column(name = "kakao_place_id", nullable = false, length = 100)
	private String kakaoPlaceId;

	@Column(name = "place_url", length = 2048)
	private String placeUrl;

	@Column(name = "place_name", length = 255)
	private String placeName;

	@Column(name = "business_hours_json", columnDefinition = "text")
	private String businessHoursJson;

	@Column(name = "business_hours_raw", columnDefinition = "text")
	private String businessHoursRaw;

	@Enumerated(EnumType.STRING)
	@Column(name = "business_hours_status", length = 30)
	private BusinessHoursStatus businessHoursStatus;

	@Column(name = "business_hours_fetched_at")
	private Instant businessHoursFetchedAt;

	@Column(name = "business_hours_expires_at")
	private Instant businessHoursExpiresAt;

	@Column(name = "business_hours_source", length = 100)
	private String businessHoursSource;

	@Column(name = "business_hours_job_id", length = 100)
	private String businessHoursJobId;

	@Enumerated(EnumType.STRING)
	@Column(name = "last_request_status", length = 30)
	private BusinessHoursRequestStatus lastRequestStatus;

	@Column(name = "last_error", columnDefinition = "text")
	private String lastError;

	@Column(name = "last_polled_at")
	private Instant lastPolledAt;

	@Version
	@Column(nullable = false)
	private Long version;

	private PlaceBusinessHours(String kakaoPlaceId, String placeUrl, String placeName) {
		this.kakaoPlaceId = trimToNull(kakaoPlaceId);
		this.placeUrl = trimToNull(placeUrl);
		this.placeName = trimToNull(placeName);
	}

	public static PlaceBusinessHours create(String kakaoPlaceId, String placeUrl, String placeName) {
		if (isBlank(kakaoPlaceId)) {
			throw new IllegalArgumentException("kakaoPlaceId is required.");
		}
		return new PlaceBusinessHours(kakaoPlaceId, placeUrl, placeName);
	}

	public void applyPlaceSnapshot(String placeUrl, String placeName) {
		this.placeUrl = chooseText(this.placeUrl, placeUrl);
		this.placeName = chooseText(this.placeName, placeName);
	}

	public void applyRemotePlace(
			String placeUrl,
			String placeName,
			String businessHoursJson,
			String businessHoursRaw,
			BusinessHoursStatus businessHoursStatus,
			Instant businessHoursFetchedAt,
			Instant businessHoursExpiresAt,
			String businessHoursSource,
			String businessHoursJobId,
			String lastError,
			BusinessHoursRequestStatus lastRequestStatus
	) {
		applyPlaceSnapshot(placeUrl, placeName);
		this.businessHoursJson = businessHoursJson;
		this.businessHoursRaw = trimToNull(businessHoursRaw);
		this.businessHoursStatus = businessHoursStatus;
		this.businessHoursFetchedAt = businessHoursFetchedAt;
		this.businessHoursExpiresAt = businessHoursExpiresAt;
		this.businessHoursSource = trimToNull(businessHoursSource);
		this.businessHoursJobId = trimToNull(businessHoursJobId);
		this.lastError = trimToNull(lastError);
		this.lastRequestStatus = lastRequestStatus;
	}

	public void markEnqueueFailed(String placeUrl, String placeName, String lastError) {
		applyPlaceSnapshot(placeUrl, placeName);
		this.businessHoursStatus = BusinessHoursStatus.ENQUEUE_FAILED;
		this.lastRequestStatus = BusinessHoursRequestStatus.ENQUEUE_FAILED;
		this.lastError = trimToNull(lastError);
	}

	public void markRequestFailure(
			String placeUrl,
			String placeName,
			BusinessHoursRequestStatus lastRequestStatus,
			String lastError
	) {
		applyPlaceSnapshot(placeUrl, placeName);
		this.lastRequestStatus = lastRequestStatus;
		this.lastError = trimToNull(lastError);
	}

	public void markPolled(Instant polledAt) {
		this.lastPolledAt = polledAt;
	}

	private static String chooseText(String current, String next) {
		String normalized = trimToNull(next);
		return normalized == null ? current : normalized;
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
