package com.hufs.capstone.backend.auth.application.service.impl;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.global.exception.FieldValidationException;
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
			throw new FieldValidationException("codeChallenge", "코드 챌린지는 필수입니다.");
		}
		if (!StringUtils.hasText(codeVerifier)) {
			throw new FieldValidationException("codeVerifier", "코드 검증값(verifier)은 필수입니다.");
		}
		if (!"S256".equalsIgnoreCase(method)) {
			throw new FieldValidationException("codeChallengeMethod", "코드 챌린지 방식은 S256만 지원합니다.", method);
		}
		String computed = s256(codeVerifier);
		if (!computed.equals(codeChallenge)) {
			throw new BusinessException(ErrorCode.PKCE_VERIFICATION_FAILED);
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




