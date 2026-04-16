package com.hufs.capstone.backend.link.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.LinkProcessingEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "link_processing_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LinkProcessingHistory extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "link_id", nullable = false)
	private Link link;

	@Column(nullable = false, length = 100)
	private String processingJobId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LinkAnalysisStatus status;

	@Column(columnDefinition = "text")
	private String captionRaw;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private LinkProcessingEventType eventType;

	@Column(length = 100)
	private String roomId;

	@Column(length = 100)
	private String source;

	private LinkProcessingHistory(
			Link link,
			String processingJobId,
			LinkAnalysisStatus status,
			String captionRaw,
			LinkProcessingEventType eventType,
			String roomId,
			String source
	) {
		this.link = link;
		this.processingJobId = processingJobId;
		this.status = status;
		this.captionRaw = captionRaw;
		this.eventType = eventType;
		this.roomId = roomId;
		this.source = source;
	}

	public static LinkProcessingHistory registered(Link link, String roomId, String source) {
		return new LinkProcessingHistory(
				link,
				link.getProcessingJobId(),
				link.getStatus(),
				link.getCaptionRaw(),
				LinkProcessingEventType.REGISTERED,
				roomId,
				source
		);
	}

	public static LinkProcessingHistory statusSynced(Link link) {
		return new LinkProcessingHistory(
				link,
				link.getProcessingJobId(),
				link.getStatus(),
				link.getCaptionRaw(),
				LinkProcessingEventType.STATUS_SYNCED,
				null,
				null
		);
	}
}
