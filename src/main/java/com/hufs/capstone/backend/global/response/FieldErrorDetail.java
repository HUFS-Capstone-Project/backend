package com.hufs.capstone.backend.global.response;

/**
 * 검증 오류를 {@link org.springframework.http.ProblemDetail} 의 {@code fieldErrors} 확장에 넣을 때 사용.
 * {@code rejectedValue} 는 비밀번호 등 민감 값이면 노출하지 않도록 호출부에서 마스킹하거나 null 로 둔다.
 */
public record FieldErrorDetail(String field, String message, String rejectedValue) {

	public static FieldErrorDetail of(String field, String message) {
		return new FieldErrorDetail(field, message, null);
	}
}
