package com.hufs.capstone.backend.room.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends AuditableEntity {

	@Column(nullable = false, unique = true, length = 36)
	private String publicId;

	@Column(nullable = false, length = 20)
	private String name;

	@Column(nullable = false, unique = true, length = 32)
	private String inviteCode;

	@Column(name = "avatar_seed", nullable = false, length = 48)
	private String avatarSeed;

	@Column(nullable = false)
	private Long createdByUserId;

	private Room(String publicId, String name, String inviteCode, String avatarSeed, Long createdByUserId) {
		this.publicId = publicId;
		this.name = name;
		this.inviteCode = inviteCode;
		this.avatarSeed = avatarSeed;
		this.createdByUserId = createdByUserId;
	}

	public static Room create(String publicId, String name, String inviteCode, Long createdByUserId) {
		return new Room(publicId, name, inviteCode, generateAvatarSeed(), createdByUserId);
	}

	public void rename(String name) {
		this.name = name;
	}

	private static String generateAvatarSeed() {
		return "room-avatar:" + UUID.randomUUID();
	}
}

