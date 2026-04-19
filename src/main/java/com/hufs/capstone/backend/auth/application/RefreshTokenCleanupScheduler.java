package com.hufs.capstone.backend.auth.application;

import com.hufs.capstone.backend.auth.domain.repository.RefreshTokenRepository;
import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

	private final RefreshTokenRepository refreshTokenRepository;
	private final AuthProperties authProperties;

	@Scheduled(cron = "0 30 3 * * *", zone = "UTC")
	@Transactional
	public void cleanupRevokedTokens() {
		Instant threshold = Instant.now().minus(authProperties.getRefresh().getCleanupRetentionDays(), ChronoUnit.DAYS);
		int deleted = refreshTokenRepository.deleteRevokedExpiredBefore(threshold);
		if (deleted > 0) {
			log.info("철회되었거나 만료된 리프레시 토큰 {}건을 삭제했습니다.", deleted);
		}
	}
}




