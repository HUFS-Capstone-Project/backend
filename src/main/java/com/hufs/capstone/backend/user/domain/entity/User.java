package com.hufs.capstone.backend.user.domain.entity;

import com.hufs.capstone.backend.global.common.entity.SoftDeletableEntity;
import com.hufs.capstone.backend.user.domain.enums.UserRole;
import com.hufs.capstone.backend.user.domain.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeletableEntity {

	@Column(length = 320)
	private String email;

	@Column(nullable = false)
	private boolean emailVerified;

	@Column(length = 60)
	private String nickname;

	@Column(length = 2048)
	private String profileImageUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserStatus status;

	@Column
	private Instant lastLoginAt;

	private User(
			String email,
			boolean emailVerified,
			String nickname,
			String profileImageUrl,
			UserRole role,
			UserStatus status,
			Instant lastLoginAt
	) {
		this.email = email;
		this.emailVerified = emailVerified;
		this.nickname = nickname;
		this.profileImageUrl = profileImageUrl;
		this.role = role;
		this.status = status;
		this.lastLoginAt = lastLoginAt;
	}

	public static User register(String email, boolean emailVerified, String nickname, String profileImageUrl) {
		Instant now = Instant.now();
		return new User(email, emailVerified, nickname, profileImageUrl, UserRole.USER, UserStatus.ACTIVE, now);
	}

	public void markLoginSuccess(Instant now) {
		this.lastLoginAt = now;
	}

	public void updateProfileImageUrl(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}

	public void changeStatus(UserStatus status) {
		this.status = status;
	}

	public boolean isActive() {
		return this.status == UserStatus.ACTIVE;
	}
}



