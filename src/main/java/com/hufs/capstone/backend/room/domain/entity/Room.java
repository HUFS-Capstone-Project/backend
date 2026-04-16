package com.hufs.capstone.backend.room.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, unique = true, length = 32)
	private String inviteCode;

	@Column(nullable = false)
	private Long createdByUserId;

	private Room(String publicId, String name, String inviteCode, Long createdByUserId) {
		this.publicId = publicId;
		this.name = name;
		this.inviteCode = inviteCode;
		this.createdByUserId = createdByUserId;
	}

	public static Room create(String publicId, String name, String inviteCode, Long createdByUserId) {
		return new Room(publicId, name, inviteCode, createdByUserId);
	}
}

