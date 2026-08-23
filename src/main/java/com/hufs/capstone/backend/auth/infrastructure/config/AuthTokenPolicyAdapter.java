package com.hufs.capstone.backend.auth.infrastructure.config;

import com.hufs.capstone.backend.auth.application.port.AuthTokenPolicy;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthTokenPolicyAdapter implements AuthTokenPolicy {

	private final AuthProperties authProperties;

	@Override
	public Duration refreshTokenTtl() {
		return authProperties.getRefresh().getTtl();
	}

	@Override
	public Duration rotationReplayWindow() {
		return authProperties.getRefresh().getRotationReplayWindow();
	}

	@Override
	public int cleanupRetentionDays() {
		return authProperties.getRefresh().getCleanupRetentionDays();
	}
}
