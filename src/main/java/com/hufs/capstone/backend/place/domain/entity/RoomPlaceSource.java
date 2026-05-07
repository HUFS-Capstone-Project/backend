package com.hufs.capstone.backend.place.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceSourceType;
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
		name = "room_place_sources",
		indexes = {
			@Index(name = "idx_room_place_sources_room_place_id", columnList = "room_place_id"),
			@Index(name = "idx_room_place_sources_room_link_id", columnList = "room_link_id")
		},
		uniqueConstraints = @UniqueConstraint(
				name = "uq_room_place_sources_room_place_id_room_link_id",
				columnNames = {"room_place_id", "room_link_id"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomPlaceSource extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_place_id", nullable = false)
	private RoomPlace roomPlace;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_link_id", nullable = false)
	private RoomLink roomLink;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 30)
	private RoomPlaceSourceType sourceType;

	@Column(name = "created_by_user_id", nullable = false)
	private Long createdByUserId;

	@Column(precision = 5, scale = 4)
	private BigDecimal confidence;

	@Column(length = 255)
	private String query;

	@Column(length = 500)
	private String evidenceText;

	@Column(columnDefinition = "text")
	private String originalText;

	private RoomPlaceSource(
			RoomPlace roomPlace,
			RoomLink roomLink,
			RoomPlaceSourceType sourceType,
			Long createdByUserId,
			PlaceSnapshot snapshot
	) {
		this.roomPlace = roomPlace;
		this.roomLink = roomLink;
		this.sourceType = sourceType;
		this.createdByUserId = createdByUserId;
		this.confidence = snapshot.confidence();
		this.query = trimToNull(snapshot.query());
		this.evidenceText = trimToNull(snapshot.evidenceText());
		this.originalText = trimToNull(snapshot.originalText());
	}

	public static RoomPlaceSource create(
			RoomPlace roomPlace,
			RoomLink roomLink,
			RoomPlaceSourceType sourceType,
			Long createdByUserId,
			PlaceSnapshot snapshot
	) {
		if (roomPlace == null || roomLink == null || sourceType == null || createdByUserId == null || snapshot == null) {
			throw new IllegalArgumentException("Room place source required values are missing.");
		}
		return new RoomPlaceSource(roomPlace, roomLink, sourceType, createdByUserId, snapshot);
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
