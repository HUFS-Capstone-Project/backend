package com.hufs.capstone.backend.place.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceSourceType;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
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
import java.math.BigDecimal;
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
			@Index(name = "idx_room_places_source_room_link_id", columnList = "source_room_link_id"),
			@Index(name = "idx_room_places_room_id_created_at", columnList = "room_id, created_at")
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

	@Column(length = 500)
	private String memo;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 30)
	private RoomPlaceSourceType sourceType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_room_link_id")
	private RoomLink sourceRoomLink;

	@Column(precision = 5, scale = 4)
	private BigDecimal confidence;

	@Column(length = 255)
	private String sourceKeyword;

	@Column(length = 500)
	private String sourceSentence;

	@Column(columnDefinition = "text")
	private String rawCandidate;

	private RoomPlace(
			Room room,
			Place place,
			Long createdByUserId,
			String memo,
			RoomPlaceSourceType sourceType,
			RoomLink sourceRoomLink,
			PlaceSnapshot snapshot
	) {
		this.room = room;
		this.place = place;
		this.createdByUserId = createdByUserId;
		this.memo = trimToNull(memo);
		this.sourceType = sourceType;
		this.sourceRoomLink = sourceRoomLink;
		this.confidence = snapshot.confidence();
		this.sourceKeyword = trimToNull(snapshot.sourceKeyword());
		this.sourceSentence = trimToNull(snapshot.sourceSentence());
		this.rawCandidate = trimToNull(snapshot.rawCandidate());
	}

	public static RoomPlace create(
			Room room,
			Place place,
			Long createdByUserId,
			String memo,
			RoomPlaceSourceType sourceType,
			RoomLink sourceRoomLink,
			PlaceSnapshot snapshot
	) {
		if (room == null || place == null || createdByUserId == null || sourceType == null || snapshot == null) {
			throw new IllegalArgumentException("Room place required values are missing.");
		}
		return new RoomPlace(room, place, createdByUserId, memo, sourceType, sourceRoomLink, snapshot);
	}

	public void updateMemo(String memo) {
		this.memo = trimToNull(memo);
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

	public Long getSourceRoomLinkId() {
		return sourceRoomLink == null ? null : sourceRoomLink.getId();
	}

	public void fillSourceRoomLinkIfAbsent(RoomLink roomLink) {
		if (this.sourceRoomLink == null) {
			this.sourceRoomLink = roomLink;
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
