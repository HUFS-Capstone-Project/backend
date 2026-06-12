package com.hufs.capstone.backend.global.pagination;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.global.exception.FieldValidationException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CursorCodec<T> {

	private final ObjectMapper objectMapper;
	private final Class<T> cursorType;

	public CursorCodec(ObjectMapper objectMapper, Class<T> cursorType) {
		this.objectMapper = objectMapper;
		this.cursorType = cursorType;
	}

	public String encode(T cursor) {
		if (cursor == null) {
			return null;
		}
		try {
			byte[] json = objectMapper.writeValueAsBytes(cursor);
			return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to encode cursor.", ex);
		}
	}

	public T decode(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		try {
			byte[] json = Base64.getUrlDecoder().decode(cursor.trim().getBytes(StandardCharsets.UTF_8));
			return objectMapper.readValue(json, cursorType);
		} catch (Exception ex) {
			throw new FieldValidationException("cursor", "Invalid cursor.", cursor);
		}
	}
}
