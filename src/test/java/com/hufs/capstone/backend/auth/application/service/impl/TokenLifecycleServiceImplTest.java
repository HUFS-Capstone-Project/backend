package com.hufs.capstone.backend.auth.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.auth.application.dto.TokenPair;
import com.hufs.capstone.backend.auth.application.port.AccessTokenIssuer;
import com.hufs.capstone.backend.auth.application.port.AccessTokenIssuer.IssuedAccessToken;
import com.hufs.capstone.backend.auth.application.port.AuthTokenPolicy;
import com.hufs.capstone.backend.auth.application.port.RefreshTokenSecurityPort;
import com.hufs.capstone.backend.auth.application.port.RotationReplayPort;
import com.hufs.capstone.backend.auth.application.service.AuthSecurityEventLogger;
import com.hufs.capstone.backend.auth.domain.entity.RefreshToken;
import com.hufs.capstone.backend.auth.domain.enums.ClientPlatform;
import com.hufs.capstone.backend.auth.domain.enums.DeviceType;
import com.hufs.capstone.backend.auth.domain.enums.RevokeReason;
import com.hufs.capstone.backend.auth.domain.repository.RefreshTokenRepository;
import com.hufs.capstone.backend.auth.domain.vo.ClientContext;
import com.hufs.capstone.backend.user.domain.entity.User;
import com.hufs.capstone.backend.user.domain.enums.UserRole;
import com.hufs.capstone.backend.user.domain.enums.UserStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenLifecycleServiceImplTest {

	private static final Instant NOW = Instant.parse("2026-08-24T00:00:00Z");
	private static final Duration REFRESH_TTL = Duration.ofDays(30);
	private static final Duration REPLAY_WINDOW = Duration.ofSeconds(15);
	private static final ClientContext CONTEXT = new ClientContext(
			DeviceType.WEB,
			ClientPlatform.REACT_WEB,
			"test-agent",
			"127.0.0.1"
	);

	private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
	private final RefreshTokenSecurityPort refreshTokenSecurity = mock(RefreshTokenSecurityPort.class);
	private final AccessTokenIssuer accessTokenIssuer = mock(AccessTokenIssuer.class);
	private final AuthTokenPolicy authTokenPolicy = mock(AuthTokenPolicy.class);
	private final RotationReplayPort rotationReplayPort = mock(RotationReplayPort.class);
	private final AuthSecurityEventLogger eventLogger = mock(AuthSecurityEventLogger.class);
	private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

	private TokenLifecycleServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new TokenLifecycleServiceImpl(
				refreshTokenRepository,
				refreshTokenSecurity,
				accessTokenIssuer,
				authTokenPolicy,
				rotationReplayPort,
				eventLogger,
				clock
		);
	}

	@Test
	void issueInitialUsesPortsAndTheInjectedClock() {
		User user = activeUser(1L);
		Instant accessExpiresAt = NOW.plusSeconds(600);
		when(accessTokenIssuer.issue(1L, UserRole.USER, UserStatus.ACTIVE, NOW))
				.thenReturn(new IssuedAccessToken("access-token", accessExpiresAt));
		when(refreshTokenSecurity.generateRawToken()).thenReturn("refresh-token");
		when(refreshTokenSecurity.hash("refresh-token")).thenReturn("refresh-hash");
		when(authTokenPolicy.refreshTokenTtl()).thenReturn(REFRESH_TTL);

		TokenPair result = service.issueInitial(user, CONTEXT);

		assertThat(result).isEqualTo(new TokenPair(
				"access-token",
				accessExpiresAt,
				"refresh-token",
				NOW.plus(REFRESH_TTL)
		));
		verify(refreshTokenRepository).save(any(RefreshToken.class));
	}

	@Test
	void rotatePersistsTheNewTokenAndReplayResultThroughPorts() {
		User user = activeUser(1L);
		RefreshToken current = mock(RefreshToken.class);
		UUID familyId = UUID.fromString("11111111-1111-1111-1111-111111111111");
		Instant accessExpiresAt = NOW.plusSeconds(600);
		when(refreshTokenSecurity.hash("old-refresh-token")).thenReturn("old-hash");
		when(refreshTokenRepository.findByTokenHashForUpdate("old-hash")).thenReturn(Optional.of(current));
		when(current.getUser()).thenReturn(user);
		when(current.getTokenFamilyId()).thenReturn(familyId);
		when(current.getId()).thenReturn(10L);
		when(refreshTokenSecurity.generateRawToken()).thenReturn("new-refresh-token");
		when(refreshTokenSecurity.hash("new-refresh-token")).thenReturn("new-hash");
		when(authTokenPolicy.refreshTokenTtl()).thenReturn(REFRESH_TTL);
		when(authTokenPolicy.rotationReplayWindow()).thenReturn(REPLAY_WINDOW);
		when(accessTokenIssuer.issue(1L, UserRole.USER, UserStatus.ACTIVE, NOW))
				.thenReturn(new IssuedAccessToken("new-access-token", accessExpiresAt));

		TokenPair result = service.rotate("old-refresh-token", CONTEXT);

		assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
		assertThat(result.refreshTokenExpiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));
		verify(current).markUsed(NOW);
		verify(current).revoke(RevokeReason.ROTATED, NOW);
		verify(refreshTokenRepository).save(any(RefreshToken.class));
		verify(rotationReplayPort).save("old-hash", CONTEXT, result, REPLAY_WINDOW);
		verify(eventLogger).logRefreshRotated(1L, familyId, CONTEXT);
	}

	private static User activeUser(Long id) {
		User user = mock(User.class);
		when(user.getId()).thenReturn(id);
		when(user.getRole()).thenReturn(UserRole.USER);
		when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
		when(user.isActive()).thenReturn(true);
		return user;
	}
}
