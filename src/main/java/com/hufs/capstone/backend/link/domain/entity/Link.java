package com.hufs.capstone.backend.link.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "links")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Link extends AuditableEntity {

	@Column(nullable = false, length = 2048)
	private String originalUrl;

	@Column(nullable = false, unique = true, length = 2048)
	private String normalizedUrl;

	@Column(nullable = false, unique = true, length = 100)
	private String processingJobId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LinkAnalysisStatus status;

	@Column(columnDefinition = "text")
	private String captionRaw;

	@Version
	@Column(nullable = false)
	private Long version;

	private Link(
			String originalUrl,
			String normalizedUrl,
			String processingJobId,
			LinkAnalysisStatus status,
			String captionRaw
	) {
		this.originalUrl = originalUrl;
		this.normalizedUrl = normalizedUrl;
		this.processingJobId = processingJobId;
		this.status = status;
		this.captionRaw = captionRaw;
	}

	public static Link register(String originalUrl, String normalizedUrl, String processingJobId) {
		return new Link(originalUrl, normalizedUrl, processingJobId, LinkAnalysisStatus.REQUESTED, null);
	}

	public boolean isTerminal() {
		return this.status.isTerminal();
	}

	public boolean markRequested() {
		if (this.status == LinkAnalysisStatus.PROCESSING) {
			return false;
		}
		return setStatusIfChanged(LinkAnalysisStatus.REQUESTED);
	}

	public boolean markProcessing() {
		return setStatusIfChanged(LinkAnalysisStatus.PROCESSING);
	}

	public boolean markFailed() {
		if (this.status == LinkAnalysisStatus.SUCCEEDED) {
			return false;
		}
		return this.status != LinkAnalysisStatus.FAILED && setStatusIfChanged(LinkAnalysisStatus.FAILED);
	}

	public boolean markSucceeded(String captionRaw) {
		boolean changed = false;
		if (this.status != LinkAnalysisStatus.SUCCEEDED) {
			this.status = LinkAnalysisStatus.SUCCEEDED;
			changed = true;
		}
		if (!Objects.equals(this.captionRaw, captionRaw)) {
			this.captionRaw = captionRaw;
			changed = true;
		}
		return changed;
	}

	private boolean setStatusIfChanged(LinkAnalysisStatus nextStatus) {
		if (this.status.isTerminal()) {
			return false;
		}
		if (this.status == nextStatus) {
			return false;
		}
		this.status = nextStatus;
		return true;
	}
}
