package com.hufs.capstone.backend.auth.application.port;

public interface RefreshTokenSecurityPort {

	String generateRawToken();

	String hash(String rawToken);
}
