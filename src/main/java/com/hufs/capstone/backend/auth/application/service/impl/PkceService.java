package com.hufs.capstone.backend.auth.application.service.impl;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PkceService {

	public void verify(String codeChallenge, String method, String codeVerifier) {
		if (!StringUtils.hasText(codeChallenge)) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "code challenge 값이 필요합니다.");
		}
		if (!StringUtils.hasText(codeVerifier)) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "code verifier 값이 필요합니다.");
		}
		if (!"S256".equalsIgnoreCase(method)) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "code challenge method는 S256만 지원합니다.");
		}
		String computed = s256(codeVerifier);
		if (!computed.equals(codeChallenge)) {
			throw new BusinessException(ErrorCode.E401_INVALID_TOKEN, "PKCE 검증에 실패했습니다.");
		}
	}

	private String s256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", ex);
		}
	}
}




