package com.hufs.capstone.backend.link.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.ProcessingDispatchStatus;
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

	@Column(length = 100)
	private String processingJobId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProcessingDispatchStatus dispatchStatus;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LinkAnalysisStatus status;

	@Column(columnDefinition = "text")
	private String contentText;

	private Long likeCount;

	private Long commentCount;

	@Column(length = 100)
	private String postedAt;

	@Column(length = 255)
	private String extractionStoreName;

	@Column(length = 500)
	private String extractionAddress;

	@Column(length = 50)
	private String extractionCertainty;

	@Column(columnDefinition = "text")
	private String extractedPlacesJson;

	@Column(columnDefinition = "text")
	private String processingResultJson;

	@Column(length = 100)
	private String errorCode;

	@Column(length = 500)
	private String errorMessage;

	private Boolean retryable;

	private Integer cooldownSeconds;

	@Version
	@Column(nullable = false)
	private Long version;

	private Link(
			String originalUrl,
			String normalizedUrl,
			String processingJobId,
			ProcessingDispatchStatus dispatchStatus,
			LinkAnalysisStatus status,
			String contentText
	) {
		this.originalUrl = originalUrl;
		this.normalizedUrl = normalizedUrl;
		this.processingJobId = processingJobId;
		this.dispatchStatus = dispatchStatus;
		this.status = status;
		this.contentText = contentText;
	}

	public static Link register(String originalUrl, String normalizedUrl, String processingJobId) {
		String normalizedProcessingJobId = (processingJobId == null || processingJobId.isBlank()) ? null : processingJobId;
		ProcessingDispatchStatus dispatchStatus = normalizedProcessingJobId == null
				? ProcessingDispatchStatus.PENDING
				: ProcessingDispatchStatus.DISPATCHED;
		return new Link(
				originalUrl,
				normalizedUrl,
				normalizedProcessingJobId,
				dispatchStatus,
				LinkAnalysisStatus.REQUESTED,
				null
		);
	}

	public static Link registerPending(String originalUrl, String normalizedUrl) {
		return new Link(
				originalUrl,
				normalizedUrl,
				null,
				ProcessingDispatchStatus.PENDING,
				LinkAnalysisStatus.REQUESTED,
				null
		);
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

	public boolean markSucceeded(String contentText) {
		boolean changed = false;
		if (this.status != LinkAnalysisStatus.SUCCEEDED) {
			this.status = LinkAnalysisStatus.SUCCEEDED;
			changed = true;
		}
		if (!Objects.equals(this.contentText, contentText)) {
			this.contentText = contentText;
			changed = true;
		}
		return changed;
	}

	public boolean isDispatchPending() {
		return dispatchStatus == ProcessingDispatchStatus.PENDING;
	}

	public boolean isDispatchReady() {
		return dispatchStatus != null && dispatchStatus.canPoll() && processingJobId != null && !processingJobId.isBlank();
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
