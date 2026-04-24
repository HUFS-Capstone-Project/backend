package com.hufs.capstone.backend.link.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.room.domain.entity.Room;
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
// TODO: 장소 최종 저장 단계에서 RoomLinkPlace(room_link + place) 매핑의 기준 엔티티로 사용한다.
@Table(
		name = "room_links",
		indexes = {
			@Index(name = "idx_room_links_link_id_room_id", columnList = "link_id, room_id")
		},
		uniqueConstraints = {
			@UniqueConstraint(name = "uq_room_links_room_id_link_id", columnNames = {"room_id", "link_id"})
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomLink extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "link_id", nullable = false)
	private Link link;

	private RoomLink(Room room, Link link) {
		this.room = room;
		this.link = link;
	}

	public static RoomLink bind(Room room, Link link) {
		return new RoomLink(room, link);
	}
}
