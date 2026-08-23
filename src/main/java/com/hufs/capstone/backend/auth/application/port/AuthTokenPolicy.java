package com.hufs.capstone.backend.auth.application.port;

import java.time.Duration;

public interface AuthTokenPolicy {

	Duration refreshTokenTtl();

	Duration rotationReplayWindow();

	int cleanupRetentionDays();
}
