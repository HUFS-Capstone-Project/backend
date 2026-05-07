package com.hufs.capstone.backend.link.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "room_link_candidate_overrides",
		indexes = {
			@Index(name = "idx_room_link_candidate_overrides_room_link_id", columnList = "room_link_id"),
			@Index(name = "idx_room_link_candidate_overrides_link_candidate_id", columnList = "link_candidate_id")
		},
		uniqueConstraints = @UniqueConstraint(
				name = "uq_room_link_candidate_overrides_room_link_candidate",
				columnNames = {"room_link_id", "link_candidate_id"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomLinkCandidateOverride extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_link_id", nullable = false)
	private RoomLink roomLink;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "link_candidate_id", nullable = false)
	private LinkCandidate linkCandidate;

	@Column(name = "created_by_user_id", nullable = false)
	private Long createdByUserId;

	@Column(name = "updated_by_user_id", nullable = false)
	private Long updatedByUserId;

	@Column(name = "kakao_place_id", nullable = false, length = 100)
	private String kakaoPlaceId;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(length = 500)
	private String categoryName;

	@Column(length = 50)
	private String categoryGroupCode;

	@Column(length = 100)
	private String categoryGroupName;

	@Column(length = 100)
	private String phone;

	@Column(length = 500)
	private String address;

	@Column(length = 500)
	private String roadAddress;

	@Column(precision = 18, scale = 15)
	private BigDecimal longitude;

	@Column(precision = 18, scale = 15)
	private BigDecimal latitude;

	@Column(length = 2048)
	private String placeUrl;

	@Column(length = 255)
	private String query;

	@Version
	@Column(nullable = false)
	private Long version;

	private RoomLinkCandidateOverride(
			RoomLink roomLink,
			LinkCandidate linkCandidate,
			Long userId,
			PlaceSnapshot snapshot
	) {
		this.roomLink = roomLink;
		this.linkCandidate = linkCandidate;
		this.createdByUserId = userId;
		update(userId, snapshot);
	}

	public static RoomLinkCandidateOverride create(
			RoomLink roomLink,
			LinkCandidate linkCandidate,
			Long userId,
			PlaceSnapshot snapshot
	) {
		validateRequired(roomLink, linkCandidate, userId, snapshot);
		return new RoomLinkCandidateOverride(roomLink, linkCandidate, userId, snapshot);
	}

	public void update(Long userId, PlaceSnapshot snapshot) {
		validateRequired(roomLink, linkCandidate, userId, snapshot);
		if (!snapshot.hasKakaoPlaceId()) {
			throw new IllegalArgumentException("kakaoPlaceId is required.");
		}
		this.updatedByUserId = userId;
		this.kakaoPlaceId = trimToNull(snapshot.kakaoPlaceId());
		this.name = trimToNull(snapshot.name());
		this.categoryName = trimToNull(snapshot.categoryName());
		this.categoryGroupCode = trimToNull(snapshot.categoryGroupCode());
		this.categoryGroupName = trimToNull(snapshot.categoryGroupName());
		this.phone = trimToNull(snapshot.phone());
		this.address = trimToNull(snapshot.address());
		this.roadAddress = trimToNull(snapshot.roadAddress());
		this.longitude = snapshot.longitude();
		this.latitude = snapshot.latitude();
		this.placeUrl = trimToNull(snapshot.placeUrl());
		this.query = trimToNull(snapshot.query());
	}

	public PlaceSnapshot toSnapshot() {
		return PlaceSnapshot.kakao(
				kakaoPlaceId,
				name,
				categoryName,
				categoryGroupCode,
				categoryGroupName,
				phone,
				address,
				roadAddress,
				longitude,
				latitude,
				placeUrl,
				null,
				query,
				null,
				null
		);
	}

	private static void validateRequired(
			RoomLink roomLink,
			LinkCandidate linkCandidate,
			Long userId,
			PlaceSnapshot snapshot
	) {
		if (roomLink == null || linkCandidate == null || userId == null || snapshot == null) {
			throw new IllegalArgumentException("Room link candidate override required values are missing.");
		}
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
