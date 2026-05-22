package com.hufs.capstone.backend.place.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceAddedVia;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import com.hufs.capstone.backend.region.application.dto.ResolvedRegion;
import com.hufs.capstone.backend.room.domain.entity.Room;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "room_places",
		indexes = {
			@Index(name = "idx_room_places_room_id", columnList = "room_id"),
			@Index(name = "idx_room_places_place_id", columnList = "place_id"),
			@Index(name = "idx_room_places_origin_room_link_id", columnList = "origin_room_link_id"),
			@Index(name = "idx_room_places_room_id_created_at", columnList = "room_id, created_at"),
			@Index(name = "idx_room_places_room_sido", columnList = "room_id, sido_code"),
			@Index(name = "idx_room_places_room_sigungu", columnList = "room_id, sigungu_code")
		},
		uniqueConstraints = {
			@UniqueConstraint(name = "uq_room_places_room_id_place_id", columnNames = {"room_id", "place_id"})
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomPlace extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", nullable = false)
	private Place place;

	@Column(name = "created_by_user_id", nullable = false)
	private Long createdByUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "added_via", nullable = false, length = 30)
	private RoomPlaceAddedVia addedVia;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "origin_room_link_id")
	private RoomLink originRoomLink;

	@Column(name = "sido_code", length = 2)
	private String sidoCode;

	@Column(name = "sido_name", length = 50)
	private String sidoName;

	@Column(name = "sigungu_code", length = 5)
	private String sigunguCode;

	@Column(name = "sigungu_name", length = 50)
	private String sigunguName;

	private RoomPlace(
			Room room,
			Place place,
			Long createdByUserId,
			RoomPlaceAddedVia addedVia,
			RoomLink originRoomLink,
			PlaceSnapshot snapshot,
			ResolvedRegion region
	) {
		this.room = room;
		this.place = place;
		this.createdByUserId = createdByUserId;
		this.addedVia = addedVia;
		this.originRoomLink = originRoomLink;
		applyRegion(region);
	}

	public static RoomPlace create(
			Room room,
			Place place,
			Long createdByUserId,
			RoomPlaceAddedVia addedVia,
			RoomLink originRoomLink,
			PlaceSnapshot snapshot,
			ResolvedRegion region
	) {
		if (room == null || place == null || createdByUserId == null || addedVia == null || snapshot == null || region == null) {
			throw new IllegalArgumentException("Room place required values are missing.");
		}
		return new RoomPlace(room, place, createdByUserId, addedVia, originRoomLink, snapshot, region);
	}

	public String getKakaoPlaceId() {
		return place.getKakaoPlaceId();
	}

	public String getPlaceName() {
		return place.getName();
	}

	public Long getPlaceId() {
		return place.getId();
	}

	public Long getOriginRoomLinkId() {
		return originRoomLink == null ? null : originRoomLink.getId();
	}

	public void fillOriginRoomLinkIfAbsent(RoomLink roomLink) {
		if (this.originRoomLink == null) {
			this.originRoomLink = roomLink;
		}
	}

	public void fillRegionIfAbsent(ResolvedRegion region) {
		if (this.sidoCode == null && region != null && region.sidoCode() != null) {
			applyRegion(region);
		}
	}

	private void applyRegion(ResolvedRegion region) {
		this.sidoCode = trimToNull(region.sidoCode());
		this.sidoName = trimToNull(region.sidoName());
		this.sigunguCode = trimToNull(region.sigunguCode());
		this.sigunguName = trimToNull(region.sigunguName());
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
