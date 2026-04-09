package com.hufs.capstone.backend.auth.application.service;

import com.hufs.capstone.backend.auth.domain.vo.ClientContext;
import java.util.UUID;

public interface AuthSecurityEventLogger {

	void logRefreshRotated(Long userId, UUID tokenFamilyId, ClientContext context);

	void logRefreshReuseDetected(Long userId, UUID tokenFamilyId, ClientContext context);

	void logOneTimeCodeConsumed(String codeType, Long userId);
}


