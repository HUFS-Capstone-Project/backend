package com.hufs.capstone.backend.link.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
		name = "room_links",
		uniqueConstraints = {
			@UniqueConstraint(name = "uq_room_links_room_id_link_id", columnNames = {"roomId", "link_id"})
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomLink extends AuditableEntity {

	@Column(nullable = false, length = 100)
	private String roomId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "link_id", nullable = false)
	private Link link;

	private RoomLink(String roomId, Link link) {
		this.roomId = roomId;
		this.link = link;
	}

	public static RoomLink bind(String roomId, Link link) {
		return new RoomLink(roomId, link);
	}
}
