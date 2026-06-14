package com.hufs.capstone.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
	E400_VALIDATION(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
	E400_BIND(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
	E400_CONSTRAINT(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
	E400_ILLEGAL_ARGUMENT(HttpStatus.BAD_REQUEST, "잘못된 요청값입니다."),
	MOBILE_LOGIN_PKCE_REQUIRED(HttpStatus.BAD_REQUEST, "모바일 로그인에는 PKCE(S256)가 필요합니다."),

	E401_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
	AUTHENTICATED_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "인증된 사용자를 찾을 수 없습니다."),
	WEB_LOGIN_TICKET_INVALID(HttpStatus.UNAUTHORIZED, "로그인 티켓이 유효하지 않거나 만료되었습니다."),
	MOBILE_AUTH_CODE_INVALID(HttpStatus.UNAUTHORIZED, "모바일 인증 코드가 유효하지 않거나 만료되었습니다."),
	REFRESH_TOKEN_COOKIE_REQUIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰 쿠키가 필요합니다."),
	REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰을 찾을 수 없습니다."),
	REFRESH_TOKEN_INACTIVE(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 더 이상 활성 상태가 아닙니다."),
	PKCE_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "PKCE 검증에 실패했습니다."),
	E401_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
	E401_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),

	E403_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	USER_ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "비활성화된 사용자 계정입니다."),
	ROOM_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "방 접근 권한이 없습니다."),
	ROOM_NOT_MEMBER(HttpStatus.FORBIDDEN, "이미 나갔거나 방 멤버가 아닙니다."),
	LINK_ANALYSIS_REQUEST_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 방의 링크 분석 요청이 아닙니다."),
	DATE_COURSE_FORBIDDEN_EDIT(HttpStatus.FORBIDDEN, "데이트 코스를 수정할 권한이 없습니다."),
	DATE_COURSE_FORBIDDEN_DELETE(HttpStatus.FORBIDDEN, "데이트 코스를 삭제할 권한이 없습니다."),

	E404_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다."),
	ROOM_PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "방 장소를 찾을 수 없습니다."),
	DATE_COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "데이트 코스를 찾을 수 없습니다."),
	DATE_COURSE_NO_PLACES(HttpStatus.NOT_FOUND, "데이트 코스 생성에 사용할 수 있는 장소가 없습니다."),
	DATE_COURSE_GENERATION_EMPTY(HttpStatus.NOT_FOUND, "생성할 수 있는 코스가 없습니다."),
	LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "링크를 찾을 수 없습니다."),
	LINK_ANALYSIS_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "링크 분석 요청을 찾을 수 없습니다."),
	LINK_CANDIDATE_NOT_FOUND(HttpStatus.NOT_FOUND, "링크 후보를 찾을 수 없습니다."),

	E409_CONFLICT(HttpStatus.CONFLICT, "요청을 처리할 수 없습니다."),
	E409_TOKEN_REUSE_DETECTED(HttpStatus.CONFLICT, "리프레시 토큰 재사용이 감지되었습니다."),
	E409_DUPLICATE_DATE_COURSE(HttpStatus.CONFLICT, "동일한 데이트 코스가 이미 저장되어 있습니다."),
	DATE_COURSE_ALREADY_SAVED(HttpStatus.CONFLICT, "이미 저장된 데이트 코스입니다."),
	ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 온보딩이 완료된 사용자입니다."),
	LINK_ANALYSIS_NOT_COMPLETED(HttpStatus.CONFLICT, "링크 분석이 완료되지 않았습니다."),
	LINK_ANALYSIS_RETRY_STATE_CHANGED(HttpStatus.CONFLICT, "재시도 중 링크 분석 요청이 변경되었습니다."),
	LINK_ANALYSIS_RETRY_NOT_ALLOWED(HttpStatus.CONFLICT, "재시도할 수 없는 링크 분석 요청입니다."),
	LINK_ANALYSIS_NOT_EXPIRED(HttpStatus.CONFLICT, "만료되지 않은 링크 분석 요청입니다."),
	ROOM_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여한 방입니다."),
	ROOM_MEMBER_LIMIT_REACHED(HttpStatus.CONFLICT, "방 인원이 가득 찼습니다. 최대 6명까지 참여할 수 있습니다."),
	ROOM_PLACE_USED_IN_DATE_COURSE(HttpStatus.CONFLICT, "저장된 데이트코스에 포함된 장소는 삭제할 수 없습니다."),
	ROOM_PLACE_SAVE_CONFLICT(HttpStatus.CONFLICT, "방 장소 저장 충돌이 발생했습니다."),
	ROOM_LINK_CREATE_CONFLICT(HttpStatus.CONFLICT, "방 링크 생성 충돌이 발생했습니다."),
	LINK_CANDIDATE_OVERRIDE_CONFLICT(HttpStatus.CONFLICT, "링크 후보 수정 충돌이 발생했습니다."),

	E429_TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
	LINK_ANALYSIS_INSTAGRAM_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "Instagram 분석이 쿨다운 중입니다. 잠시 후 다시 시도해주세요."),

	E502_EXTERNAL_API(HttpStatus.BAD_GATEWAY, "외부 API 호출에 실패했습니다."),
	E500_INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

	private final HttpStatus httpStatus;
	private final String defaultMessage;

	ErrorCode(HttpStatus httpStatus, String defaultMessage) {
		this.httpStatus = httpStatus;
		this.defaultMessage = defaultMessage;
	}
}
