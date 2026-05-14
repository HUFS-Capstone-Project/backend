package com.hufs.capstone.backend.place.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
		name = "room_place_memos",
		indexes = {
			@Index(name = "idx_room_place_memos_room_place_id", columnList = "room_place_id"),
			@Index(name = "idx_room_place_memos_user_id", columnList = "user_id")
		},
		uniqueConstraints = {
			@UniqueConstraint(name = "uq_room_place_memos_room_place_id_user_id", columnNames = {"room_place_id", "user_id"})
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomPlaceMemo extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_place_id", nullable = false)
	private RoomPlace roomPlace;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, length = 500)
	private String memo;

	private RoomPlaceMemo(RoomPlace roomPlace, Long userId, String memo) {
		this.roomPlace = roomPlace;
		this.userId = userId;
		this.memo = requireMemo(memo);
	}

	public static RoomPlaceMemo create(RoomPlace roomPlace, Long userId, String memo) {
		if (roomPlace == null || userId == null) {
			throw new IllegalArgumentException("Room place memo required values are missing.");
		}
		return new RoomPlaceMemo(roomPlace, userId, memo);
	}

	public void update(String memo) {
		this.memo = requireMemo(memo);
	}

	public Long getRoomPlaceId() {
		return roomPlace.getId();
	}

	private static String requireMemo(String value) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			throw new IllegalArgumentException("Room place memo must not be blank.");
		}
		return trimmed;
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
