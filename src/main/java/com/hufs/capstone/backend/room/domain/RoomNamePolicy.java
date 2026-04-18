package com.hufs.capstone.backend.room.domain;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;

public final class RoomNamePolicy {

	public static final int MAX_LENGTH = 20;

	private RoomNamePolicy() {
	}

	public static String normalizeAndValidate(String roomName) {
		if (roomName == null || roomName.isBlank()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "방 이름은 필수입니다.");
		}
		String normalized = roomName.trim();
		if (normalized.isEmpty()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "방 이름은 공백만 입력할 수 없습니다.");
		}
		if (normalized.length() > MAX_LENGTH) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "방 이름은 20자를 초과할 수 없습니다.");
		}
		return normalized;
	}
}
