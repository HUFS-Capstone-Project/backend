package com.hufs.capstone.backend.auth.application.service.impl;

import com.hufs.capstone.backend.auth.application.service.AuthSecurityEventLogger;
import com.hufs.capstone.backend.auth.domain.vo.ClientContext;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthSecurityEventLoggerImpl implements AuthSecurityEventLogger {

	@Override
	public void logRefreshRotated(Long userId, UUID tokenFamilyId, ClientContext context) {
		log.info(
				"auth_event=refresh_rotated userId={} familyId={} deviceType={} clientPlatform={} ip={}",
				userId,
				tokenFamilyId,
				context.deviceType(),
				context.clientPlatform(),
				context.ipAddress()
		);
	}

	@Override
	public void logRefreshReuseDetected(Long userId, UUID tokenFamilyId, ClientContext context) {
		log.warn(
				"auth_event=refresh_reuse_detected userId={} familyId={} deviceType={} clientPlatform={} ip={}",
				userId,
				tokenFamilyId,
				context.deviceType(),
				context.clientPlatform(),
				context.ipAddress()
		);
	}

	@Override
	public void logOneTimeCodeConsumed(String codeType, Long userId) {
		log.info("auth_event=one_time_code_consumed type={} userId={}", codeType, userId);
	}
}
