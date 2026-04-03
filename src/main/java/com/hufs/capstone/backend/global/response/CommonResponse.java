package com.hufs.capstone.backend.global.response;

import java.time.Instant;

public record CommonResponse<T>(
		boolean success,
		T data,
		String message,
		Instant timestamp
) {

	public static <T> CommonResponse<T> ok(T data) {
		return new CommonResponse<>(true, data, null, Instant.now());
	}

	public static <T> CommonResponse<T> ok(T data, String message) {
		return new CommonResponse<>(true, data, message, Instant.now());
	}

	/** 본문 없이 메시지만 보낼 때 */
	public static CommonResponse<Void> okMessage(String message) {
		return new CommonResponse<>(true, null, message, Instant.now());
	}
}
