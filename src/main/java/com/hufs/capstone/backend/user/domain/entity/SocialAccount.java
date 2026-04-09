package com.hufs.capstone.backend.user.domain.entity;

import com.hufs.capstone.backend.global.common.entity.SoftDeletableEntity;
import com.hufs.capstone.backend.user.domain.enums.SocialProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "social_accounts",
		uniqueConstraints = {
			@UniqueConstraint(name = "uq_provider_subject", columnNames = {"provider", "providerUserId"})
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends SoftDeletableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SocialProvider provider;

	@Column(nullable = false, length = 191)
	private String providerUserId;

	@Column(length = 320)
	private String providerEmail;

	@Column
	private Boolean providerEmailVerified;

	@Column(nullable = false)
	private Instant linkedAt;

	@Column
	private Instant lastLoginAt;

	private SocialAccount(
			User user,
			SocialProvider provider,
			String providerUserId,
			String providerEmail,
			Boolean providerEmailVerified,
			Instant linkedAt,
			Instant lastLoginAt
	) {
		this.user = user;
		this.provider = provider;
		this.providerUserId = providerUserId;
		this.providerEmail = providerEmail;
		this.providerEmailVerified = providerEmailVerified;
		this.linkedAt = linkedAt;
		this.lastLoginAt = lastLoginAt;
	}

	public static SocialAccount link(
			User user,
			SocialProvider provider,
			String providerUserId,
			String providerEmail,
			Boolean providerEmailVerified
	) {
		Instant now = Instant.now();
		return new SocialAccount(user, provider, providerUserId, providerEmail, providerEmailVerified, now, now);
	}

	public void markLogin(Instant now) {
		this.lastLoginAt = now;
	}

	public void updateProviderProfile(String providerEmail, Boolean providerEmailVerified) {
		this.providerEmail = providerEmail;
		this.providerEmailVerified = providerEmailVerified;
	}
}


