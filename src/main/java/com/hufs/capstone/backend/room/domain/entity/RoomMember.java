package com.hufs.capstone.backend.room.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.room.domain.RoomMemberRole;
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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RoomMemberRole role;

	private RoomMember(Room room, Long userId, RoomMemberRole role) {
		this.room = room;
		this.userId = userId;
		this.role = role;
	}

	public static RoomMember owner(Room room, Long userId) {
		return new RoomMember(room, userId, RoomMemberRole.OWNER);
	}

	public static RoomMember member(Room room, Long userId) {
		return new RoomMember(room, userId, RoomMemberRole.MEMBER);
	}
}
