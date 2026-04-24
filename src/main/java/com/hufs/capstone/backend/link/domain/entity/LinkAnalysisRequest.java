package com.hufs.capstone.backend.link.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.room.domain.entity.Room;
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
		name = "link_analysis_requests",
		indexes = {
			@Index(name = "idx_link_analysis_requests_link_id_room_id", columnList = "link_id, room_id")
		},
		uniqueConstraints = {
			@UniqueConstraint(name = "uq_link_analysis_requests_room_id_link_id", columnNames = {"room_id", "link_id"})
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LinkAnalysisRequest extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "link_id", nullable = false)
	private Link link;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@Column(nullable = false)
	private Long requestedBy;

	@Column(length = 100)
	private String source;

	private LinkAnalysisRequest(Link link, Room room, Long requestedBy, String source) {
		this.link = link;
		this.room = room;
		this.requestedBy = requestedBy;
		this.source = source;
	}

	public static LinkAnalysisRequest create(Link link, Room room, Long requestedBy, String source) {
		return new LinkAnalysisRequest(link, room, requestedBy, source);
	}
}
