package com.hufs.capstone.backend.link.application.dto;

/**
 * 링크 등록 요청을 application 계층에 전달하기 위한 커맨드.
 * <p>
 * TODO: 입력 검증(필수 필드, URL 형식, 길이 제한)을 controller DTO에서 할지, 여기서 할지,
 * Bean Validation을 어디에 둘지 정책이 확정되면 반영한다.
 */
public record RegisterLinkCommand(
		String url,
		String roomId,
		String source
) {
}
