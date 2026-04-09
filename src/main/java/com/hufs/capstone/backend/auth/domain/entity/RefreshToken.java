package com.hufs.capstone.backend.auth.domain.entity;

import com.hufs.capstone.backend.auth.domain.enums.ClientPlatform;
import com.hufs.capstone.backend.auth.domain.enums.DeviceType;
import com.hufs.capstone.backend.auth.domain.enums.RevokeReason;
import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.user.domain.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "auth_refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(nullable = false, columnDefinition = "uuid")
	private UUID tokenFamilyId;

	@Column
	private Long rotatedFromTokenId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DeviceType deviceType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ClientPlatform clientPlatform;

	@Column(length = 500)
	private String userAgent;

	@Column(length = 50)
	private String ipAddress;

	@Column(nullable = false)
	private Instant issuedAt;

	@Column
	private Instant lastUsedAt;

	@Column(nullable = false)
	private Instant expiresAt;

	@Column
	private Instant revokedAt;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private RevokeReason revokeReason;

	@Column(nullable = false)
	private boolean compromised;

	private RefreshToken(
			User user,
			String tokenHash,
			UUID tokenFamilyId,
			Long rotatedFromTokenId,
			DeviceType deviceType,
			ClientPlatform clientPlatform,
			String userAgent,
			String ipAddress,
			Instant issuedAt,
			Instant expiresAt
	) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.tokenFamilyId = tokenFamilyId;
		this.rotatedFromTokenId = rotatedFromTokenId;
		this.deviceType = deviceType;
		this.clientPlatform = clientPlatform;
		this.userAgent = userAgent;
		this.ipAddress = ipAddress;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
		this.lastUsedAt = issuedAt;
		this.compromised = false;
	}

	public static RefreshToken issue(
			User user,
			String tokenHash,
			UUID tokenFamilyId,
			Long rotatedFromTokenId,
			DeviceType deviceType,
			ClientPlatform clientPlatform,
			String userAgent,
			String ipAddress,
			Instant issuedAt,
			Instant expiresAt
	) {
		return new RefreshToken(
				user,
				tokenHash,
				tokenFamilyId,
				rotatedFromTokenId,
				deviceType,
				clientPlatform,
				userAgent,
				ipAddress,
				issuedAt,
				expiresAt
		);
	}

	public void markUsed(Instant now) {
		this.lastUsedAt = now;
	}

	public void revoke(RevokeReason reason, Instant now) {
		if (this.revokedAt != null) {
			return;
		}
		this.revokedAt = now;
		this.revokeReason = reason;
	}

	public void markCompromised() {
		this.compromised = true;
	}

	public boolean isExpired(Instant now) {
		return now.isAfter(this.expiresAt);
	}

	public boolean isRevoked() {
		return this.revokedAt != null;
	}

	public boolean isActive(Instant now) {
		return !isRevoked() && !isExpired(now);
	}
}



