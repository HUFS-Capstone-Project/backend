package com.hufs.capstone.backend.auth.application.service.impl;

import com.hufs.capstone.backend.auth.application.service.AuthSecurityEventLogger;
import com.hufs.capstone.backend.auth.application.service.TokenLifecycleService;
import com.hufs.capstone.backend.auth.application.port.AccessTokenIssuer;
import com.hufs.capstone.backend.auth.application.port.AccessTokenIssuer.IssuedAccessToken;
import com.hufs.capstone.backend.auth.application.port.AuthTokenPolicy;
import com.hufs.capstone.backend.auth.application.port.RefreshTokenSecurityPort;
import com.hufs.capstone.backend.auth.application.port.RotationReplayPort;
import com.hufs.capstone.backend.auth.domain.entity.RefreshToken;
import com.hufs.capstone.backend.auth.domain.enums.RevokeReason;
import com.hufs.capstone.backend.auth.domain.repository.RefreshTokenRepository;
import com.hufs.capstone.backend.auth.domain.vo.ClientContext;
import com.hufs.capstone.backend.auth.application.dto.TokenPair;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.user.domain.entity.User;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenLifecycleServiceImpl implements TokenLifecycleService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final RefreshTokenSecurityPort refreshTokenSecurity;
	private final AccessTokenIssuer accessTokenIssuer;
	private final AuthTokenPolicy authTokenPolicy;
	private final RotationReplayPort rotationReplayPort;
	private final AuthSecurityEventLogger authSecurityEventLogger;
	private final Clock clock;

	@Override
	@Transactional
	public TokenPair issueInitial(User user, ClientContext context) {
		Instant now = clock.instant();
		IssuedAccessToken accessToken = accessTokenIssuer.issue(user.getId(), user.getRole(), user.getStatus(), now);

		String rawRefreshToken = refreshTokenSecurity.generateRawToken();
		String tokenHash = refreshTokenSecurity.hash(rawRefreshToken);
		Instant refreshExpiresAt = now.plus(authTokenPolicy.refreshTokenTtl());
		RefreshToken refreshToken = RefreshToken.issue(
				user,
				tokenHash,
				UUID.randomUUID(),
				null,
				context.deviceType(),
				context.clientPlatform(),
				context.userAgent(),
				context.ipAddress(),
				now,
				refreshExpiresAt
		);
		refreshTokenRepository.save(refreshToken);

		return new TokenPair(accessToken.value(), accessToken.expiresAt(), rawRefreshToken, refreshExpiresAt);
	}

	@Override
	@Transactional
	public TokenPair rotate(String presentedRefreshToken, ClientContext context) {
		Instant now = clock.instant();
		String tokenHash = refreshTokenSecurity.hash(presentedRefreshToken);
		RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
				.orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

		if (current.isRevoked() || current.isExpired(now)) {
			TokenPair replay = rotationReplayPort.findReplay(tokenHash, context);
			if (replay != null) {
				return replay;
			}
			if (current.isRevoked() && current.getRevokeReason() == RevokeReason.ROTATED) {
				revokeFamily(current.getTokenFamilyId(), now, RevokeReason.REUSE_DETECTED, true);
				authSecurityEventLogger.logRefreshReuseDetected(current.getUser().getId(), current.getTokenFamilyId(), context);
				throw new BusinessException(ErrorCode.E409_TOKEN_REUSE_DETECTED);
			}
			throw new BusinessException(ErrorCode.REFRESH_TOKEN_INACTIVE);
		}

		current.markUsed(now);
		current.revoke(RevokeReason.ROTATED, now);
		User user = current.getUser();
		if (!user.isActive()) {
			throw new BusinessException(ErrorCode.USER_ACCOUNT_DISABLED);
		}

		String newRawRefreshToken = refreshTokenSecurity.generateRawToken();
		String newHash = refreshTokenSecurity.hash(newRawRefreshToken);
		Instant refreshExpiresAt = now.plus(authTokenPolicy.refreshTokenTtl());
		RefreshToken rotated = RefreshToken.issue(
				user,
				newHash,
				current.getTokenFamilyId(),
				current.getId(),
				context.deviceType(),
				context.clientPlatform(),
				context.userAgent(),
				context.ipAddress(),
				now,
				refreshExpiresAt
		);
		refreshTokenRepository.save(rotated);

		IssuedAccessToken accessToken = accessTokenIssuer.issue(user.getId(), user.getRole(), user.getStatus(), now);
		TokenPair result = new TokenPair(accessToken.value(), accessToken.expiresAt(), newRawRefreshToken, refreshExpiresAt);
		rotationReplayPort.save(tokenHash, context, result, authTokenPolicy.rotationReplayWindow());
		authSecurityEventLogger.logRefreshRotated(user.getId(), current.getTokenFamilyId(), context);
		return result;
	}

	@Override
	@Transactional
	public void revokeByRawToken(String presentedRefreshToken, RevokeReason reason) {
		String tokenHash = refreshTokenSecurity.hash(presentedRefreshToken);
		refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
				.ifPresent(token -> token.revoke(reason, clock.instant()));
	}

	@Override
	@Transactional
	public void revokeAllByUserId(Long userId, RevokeReason reason) {
		Instant now = clock.instant();
		List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(userId, now);
		activeTokens.forEach(token -> token.revoke(reason, now));
	}

	@Transactional
	public void revokeFamily(UUID familyId, Instant now, RevokeReason reason, boolean markCompromised) {
		List<RefreshToken> family = refreshTokenRepository.findByTokenFamilyIdAndRevokedAtIsNull(familyId);
		for (RefreshToken refreshToken : family) {
			refreshToken.revoke(reason, now);
			if (markCompromised) {
				refreshToken.markCompromised();
			}
		}
	}
}



