package com.hufs.capstone.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
	E400_VALIDATION(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
	E400_BIND(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
	E400_CONSTRAINT(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
	E400_ILLEGAL_ARGUMENT(HttpStatus.BAD_REQUEST, "잘못된 요청값입니다."),
	E401_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
	E401_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
	E401_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
	E429_TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
	E403_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	E404_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
	E409_CONFLICT(HttpStatus.CONFLICT, "요청을 처리할 수 없습니다."),
	E409_TOKEN_REUSE_DETECTED(HttpStatus.CONFLICT, "리프레시 토큰 재사용이 감지되었습니다."),
	E409_DUPLICATE_DATE_COURSE(HttpStatus.CONFLICT, "동일한 데이트 코스가 이미 저장되어 있습니다."),
	E502_EXTERNAL_API(HttpStatus.BAD_GATEWAY, "외부 API 호출에 실패했습니다."),
	E500_INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

	private final HttpStatus httpStatus;
	private final String defaultMessage;

	ErrorCode(HttpStatus httpStatus, String defaultMessage) {
		this.httpStatus = httpStatus;
		this.defaultMessage = defaultMessage;
	}
}

