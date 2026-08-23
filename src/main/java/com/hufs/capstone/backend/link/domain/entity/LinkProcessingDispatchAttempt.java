package com.hufs.capstone.backend.link.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.link.domain.LinkProcessingDispatchAttemptStatus;
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
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Table(
		name = "link_processing_dispatch_attempts",
		indexes = {
			@Index(name = "idx_link_dispatch_attempts_recovery",
					columnList = "active_slot, status, claimed_at, created_at")
		},
		uniqueConstraints = {
			@UniqueConstraint(name = "uq_link_dispatch_attempts_active",
					columnNames = {"link_id", "active_slot"})
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LinkProcessingDispatchAttempt extends AuditableEntity {

	private static final int ACTIVE_SLOT = 1;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "link_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Link link;

	@Column(nullable = false, length = 2048)
	private String originalUrl;

	@Column(nullable = false, length = 100)
	private String roomId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LinkProcessingDispatchAttemptStatus status;

	@Column(name = "active_slot")
	private Integer activeSlot;

	@Column(name = "claim_token", length = 36)
	private String claimToken;

	@Column(name = "claimed_at")
	private Instant claimedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "processing_job_id", length = 100)
	private String processingJobId;

	@Version
	@Column(nullable = false)
	private Long version;

	private LinkProcessingDispatchAttempt(Link link, String originalUrl, String roomId) {
		this.link = Objects.requireNonNull(link);
		this.originalUrl = requireText(originalUrl, "originalUrl");
		this.roomId = requireText(roomId, "roomId");
		this.status = LinkProcessingDispatchAttemptStatus.PENDING;
		this.activeSlot = ACTIVE_SLOT;
	}

	public static LinkProcessingDispatchAttempt create(Link link, String originalUrl, String roomId) {
		return new LinkProcessingDispatchAttempt(link, originalUrl, roomId);
	}

	public boolean claim(String nextClaimToken, Instant now, Instant staleBefore) {
		if (!isClaimable(staleBefore)) {
			return false;
		}
		this.status = LinkProcessingDispatchAttemptStatus.DISPATCHING;
		this.claimToken = requireText(nextClaimToken, "claimToken");
		this.claimedAt = Objects.requireNonNull(now);
		return true;
	}

	public boolean isOwnedBy(String expectedClaimToken) {
		return status == LinkProcessingDispatchAttemptStatus.DISPATCHING
				&& activeSlot != null
				&& Objects.equals(claimToken, expectedClaimToken);
	}

	public boolean markDispatched(String expectedClaimToken, String jobId, Instant now) {
		if (!isOwnedBy(expectedClaimToken)) {
			return false;
		}
		this.status = LinkProcessingDispatchAttemptStatus.DISPATCHED;
		this.processingJobId = requireText(jobId, "processingJobId");
		complete(now);
		return true;
	}

	public boolean markFailed(String expectedClaimToken, Instant now) {
		if (!isOwnedBy(expectedClaimToken)) {
			return false;
		}
		this.status = LinkProcessingDispatchAttemptStatus.FAILED;
		complete(now);
		return true;
	}

	private boolean isClaimable(Instant staleBefore) {
		if (activeSlot == null) {
			return false;
		}
		if (status == LinkProcessingDispatchAttemptStatus.PENDING) {
			return true;
		}
		return status == LinkProcessingDispatchAttemptStatus.DISPATCHING
				&& (claimedAt == null || !claimedAt.isAfter(staleBefore));
	}

	private void complete(Instant now) {
		this.activeSlot = null;
		this.claimToken = null;
		this.completedAt = Objects.requireNonNull(now);
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value;
	}
}
