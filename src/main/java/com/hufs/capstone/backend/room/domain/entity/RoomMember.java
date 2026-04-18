package com.hufs.capstone.backend.room.domain.entity;

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
		name = "room_members",
		indexes = {
			@Index(name = "idx_room_members_user_id_room_id", columnList = "user_id, room_id")
		},
		uniqueConstraints = {
			@UniqueConstraint(name = "uq_room_members_room_id_user_id", columnNames = {"room_id", "user_id"})
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMember extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false)
	private boolean pinned;

	private RoomMember(Room room, Long userId) {
		this.room = room;
		this.userId = userId;
		this.pinned = false;
	}

	public static RoomMember join(Room room, Long userId) {
		return new RoomMember(room, userId);
	}

	public void updatePinned(boolean pinned) {
		this.pinned = pinned;
	}
}
