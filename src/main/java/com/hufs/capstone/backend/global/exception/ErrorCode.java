package com.hufs.capstone.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
	E400_VALIDATION(HttpStatus.BAD_REQUEST, "Request body validation failed."),
	E400_BIND(HttpStatus.BAD_REQUEST, "Request binding failed."),
	E400_CONSTRAINT(HttpStatus.BAD_REQUEST, "Constraint validation failed."),
	E400_ILLEGAL_ARGUMENT(HttpStatus.BAD_REQUEST, "Invalid argument."),
	E401_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
	E401_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token."),
	E401_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token has expired."),
	E429_TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Too many requests."),
	E403_FORBIDDEN(HttpStatus.FORBIDDEN, "Access is denied."),
	E404_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found."),
	E409_CONFLICT(HttpStatus.CONFLICT, "Business conflict occurred."),
	E409_TOKEN_REUSE_DETECTED(HttpStatus.CONFLICT, "Refresh token reuse detected."),
	E502_EXTERNAL_API(HttpStatus.BAD_GATEWAY, "External API call failed."),
	E500_INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.");

	private final HttpStatus httpStatus;
	private final String defaultMessage;

	ErrorCode(HttpStatus httpStatus, String defaultMessage) {
		this.httpStatus = httpStatus;
		this.defaultMessage = defaultMessage;
	}
}



