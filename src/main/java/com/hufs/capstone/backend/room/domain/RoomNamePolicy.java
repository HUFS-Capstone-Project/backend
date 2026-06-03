package com.hufs.capstone.backend.room.domain;

import com.hufs.capstone.backend.global.exception.FieldValidationException;

public final class RoomNamePolicy {

	public static final int MAX_LENGTH = 20;

	private RoomNamePolicy() {
	}

	public static String normalizeAndValidate(String roomName) {
		if (roomName == null || roomName.isBlank()) {
			throw new FieldValidationException("name", "방 이름은 필수입니다.");
		}
		String normalized = roomName.trim();
		if (normalized.length() > MAX_LENGTH) {
			throw new FieldValidationException("name", "방 이름은 20자를 초과할 수 없습니다.", normalized);
		}
		return normalized;
	}
}
