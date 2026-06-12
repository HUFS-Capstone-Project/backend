package com.hufs.capstone.backend.place.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.place.domain.enums.PlaceSource;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "places",
		indexes = {
			@Index(name = "idx_places_source_external_place_id", columnList = "source, external_place_id"),
			@Index(name = "idx_places_service_category_id", columnList = "service_category_id"),
			@Index(name = "idx_places_service_tag_id", columnList = "service_tag_id"),
			@Index(name = "idx_places_lat_lng", columnList = "latitude, longitude")
		},
		uniqueConstraints = {
			@UniqueConstraint(name = "uq_places_source_external_place_id", columnNames = {"source", "external_place_id"}),
			@UniqueConstraint(name = "uq_places_kakao_place_id", columnNames = "kakao_place_id")
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends AuditableEntity {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PlaceSource source;

	@Column(name = "external_place_id", nullable = false, length = 100)
	private String externalPlaceId;

	@Column(name = "kakao_place_id", nullable = false, length = 100)
	private String kakaoPlaceId;

	@Column(length = 255)
	private String name;

	@Column(name = "category_name", length = 500)
	private String categoryName;

	@Column(name = "category_group_code", length = 50)
	private String categoryGroupCode;

	@Column(length = 100)
	private String phone;

	@Column(length = 500)
	private String address;

	@Column(name = "road_address", length = 500)
	private String roadAddress;

	@Column(precision = 18, scale = 15)
	private BigDecimal longitude;

	@Column(precision = 18, scale = 15)
	private BigDecimal latitude;

	@Column(name = "place_url", length = 2048)
	private String placeUrl;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "service_category_id", nullable = false)
	private PlaceCategory serviceCategory;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "service_tag_id", nullable = false)
	private PlaceTag serviceTag;

	private Place(PlaceSnapshot snapshot, PlaceCategory serviceCategory, PlaceTag serviceTag) {
		this.source = snapshot.source();
		this.externalPlaceId = trimToNull(snapshot.externalPlaceId());
		this.kakaoPlaceId = trimToNull(snapshot.kakaoPlaceId());
		this.serviceCategory = serviceCategory;
		this.serviceTag = serviceTag;
		applySnapshot(snapshot);
	}

	public static Place create(PlaceSnapshot snapshot, PlaceCategory serviceCategory, PlaceTag serviceTag) {
		if (snapshot.source() == null) {
			throw new IllegalArgumentException("Place source is required.");
		}
		if (isBlank(snapshot.externalPlaceId())) {
			throw new IllegalArgumentException("externalPlaceId is required.");
		}
		if (isBlank(snapshot.kakaoPlaceId())) {
			throw new IllegalArgumentException("kakaoPlaceId is required.");
		}
		if (serviceCategory == null || serviceTag == null) {
			throw new IllegalArgumentException("Service taxonomy is required.");
		}
		return new Place(snapshot, serviceCategory, serviceTag);
	}

	public void updateFrom(PlaceSnapshot snapshot, PlaceCategory serviceCategory, PlaceTag serviceTag) {
		applySnapshot(snapshot);
		if (serviceCategory != null && serviceTag != null) {
			this.serviceCategory = serviceCategory;
			this.serviceTag = serviceTag;
		}
	}

	private void applySnapshot(PlaceSnapshot snapshot) {
		this.name = chooseText(this.name, snapshot.name());
		this.categoryName = chooseText(this.categoryName, snapshot.categoryName());
		this.categoryGroupCode = chooseText(this.categoryGroupCode, snapshot.categoryGroupCode());
		this.phone = chooseText(this.phone, snapshot.phone());
		this.address = chooseText(this.address, snapshot.address());
		this.roadAddress = chooseText(this.roadAddress, snapshot.roadAddress());
		this.placeUrl = chooseText(this.placeUrl, snapshot.placeUrl());
		if (snapshot.longitude() != null && snapshot.latitude() != null) {
			this.longitude = snapshot.longitude();
			this.latitude = snapshot.latitude();
		}
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
