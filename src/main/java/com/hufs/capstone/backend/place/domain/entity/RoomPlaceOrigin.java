package com.hufs.capstone.backend.place.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceAddedVia;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "room_place_origins",
		indexes = {
			@Index(name = "idx_room_place_origins_room_place_id", columnList = "room_place_id"),
			@Index(name = "idx_room_place_origins_room_link_id", columnList = "room_link_id")
		},
		uniqueConstraints = @UniqueConstraint(
				name = "uq_room_place_origins_room_place_id_room_link_id",
				columnNames = {"room_place_id", "room_link_id"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomPlaceOrigin extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_place_id", nullable = false)
	private RoomPlace roomPlace;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_link_id", nullable = false)
	private RoomLink roomLink;

	@Enumerated(EnumType.STRING)
	@Column(name = "added_via", nullable = false, length = 30)
	private RoomPlaceAddedVia addedVia;

	@Column(name = "created_by_user_id", nullable = false)
	private Long createdByUserId;

	private RoomPlaceOrigin(
			RoomPlace roomPlace,
			RoomLink roomLink,
			RoomPlaceAddedVia addedVia,
			Long createdByUserId,
			PlaceSnapshot snapshot
	) {
		this.roomPlace = roomPlace;
		this.roomLink = roomLink;
		this.addedVia = addedVia;
		this.createdByUserId = createdByUserId;
	}

	public static RoomPlaceOrigin create(
			RoomPlace roomPlace,
			RoomLink roomLink,
			RoomPlaceAddedVia addedVia,
			Long createdByUserId,
			PlaceSnapshot snapshot
	) {
		if (roomPlace == null || roomLink == null || addedVia == null || createdByUserId == null || snapshot == null) {
			throw new IllegalArgumentException("Room place origin required values are missing.");
		}
		return new RoomPlaceOrigin(roomPlace, roomLink, addedVia, createdByUserId, snapshot);
	}
}
