package com.hufs.capstone.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
	E400_VALIDATION(HttpStatus.BAD_REQUEST, "입력 값이 올바르지 않습니다."),
	E400_BIND(HttpStatus.BAD_REQUEST, "요청 바인딩에 실패했습니다."),
	E400_CONSTRAINT(HttpStatus.BAD_REQUEST, "제약 조건을 위반했습니다."),
	E400_ILLEGAL_ARGUMENT(HttpStatus.BAD_REQUEST, "잘못된 인자입니다."),
	E404_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
	E409_CONFLICT(HttpStatus.CONFLICT, "비즈니스 규칙 충돌이 발생했습니다."),
	E502_EXTERNAL_API(HttpStatus.BAD_GATEWAY, "외부 연동에 실패했습니다."),
	E500_INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

	private final HttpStatus httpStatus;
	private final String defaultMessage;

	ErrorCode(HttpStatus httpStatus, String defaultMessage) {
		this.httpStatus = httpStatus;
		this.defaultMessage = defaultMessage;
	}
}
