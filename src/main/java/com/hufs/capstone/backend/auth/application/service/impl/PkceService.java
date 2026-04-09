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
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "Missing code challenge.");
		}
		if (!StringUtils.hasText(codeVerifier)) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "Missing code verifier.");
		}
		if (!"S256".equalsIgnoreCase(method)) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "Only S256 code challenge method is supported.");
		}
		String computed = s256(codeVerifier);
		if (!computed.equals(codeChallenge)) {
			throw new BusinessException(ErrorCode.E401_INVALID_TOKEN, "PKCE verification failed.");
		}
	}

	private String s256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 algorithm unavailable.", ex);
		}
	}
}



