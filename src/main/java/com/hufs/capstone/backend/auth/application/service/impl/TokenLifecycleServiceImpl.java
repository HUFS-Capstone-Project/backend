package com.hufs.capstone.backend.auth.application.service.impl;

import com.hufs.capstone.backend.auth.application.service.AuthSecurityEventLogger;
import com.hufs.capstone.backend.auth.application.service.TokenLifecycleService;
import com.hufs.capstone.backend.auth.domain.entity.RefreshToken;
import com.hufs.capstone.backend.auth.domain.enums.RevokeReason;
import com.hufs.capstone.backend.auth.domain.repository.RefreshTokenRepository;
import com.hufs.capstone.backend.auth.domain.vo.ClientContext;
import com.hufs.capstone.backend.auth.application.dto.TokenPair;
import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import com.hufs.capstone.backend.auth.infrastructure.security.JwtTokenProvider;
import com.hufs.capstone.backend.auth.infrastructure.security.RefreshTokenGenerator;
import com.hufs.capstone.backend.auth.infrastructure.security.RefreshTokenHasher;
import com.hufs.capstone.backend.auth.infrastructure.store.RefreshRotationReplayStore;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.user.domain.entity.User;
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
	private final RefreshTokenGenerator refreshTokenGenerator;
	private final RefreshTokenHasher refreshTokenHasher;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthProperties authProperties;
	private final RefreshRotationReplayStore refreshRotationReplayStore;
	private final AuthSecurityEventLogger authSecurityEventLogger;

	@Override
	@Transactional
	public TokenPair issueInitial(User user, ClientContext context) {
		Instant now = Instant.now();
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole(), user.getStatus(), now);
		Instant accessExpiresAt = jwtTokenProvider.getAccessTokenExpiresAt(now);

		String rawRefreshToken = refreshTokenGenerator.generate();
		String tokenHash = refreshTokenHasher.hash(rawRefreshToken);
		Instant refreshExpiresAt = now.plus(authProperties.getRefresh().getTtl());
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

		return new TokenPair(accessToken, accessExpiresAt, rawRefreshToken, refreshExpiresAt);
	}

	@Override
	@Transactional
	public TokenPair rotate(String presentedRefreshToken, ClientContext context) {
		Instant now = Instant.now();
		String tokenHash = refreshTokenHasher.hash(presentedRefreshToken);
		RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
				.orElseThrow(() -> new BusinessException(ErrorCode.E401_INVALID_TOKEN, "리프레시 토큰을 찾을 수 없습니다."));

		if (current.isRevoked() || current.isExpired(now)) {
			TokenPair replay = refreshRotationReplayStore.findReplay(tokenHash, context);
			if (replay != null) {
				return replay;
			}
			if (current.isRevoked() && current.getRevokeReason() == RevokeReason.ROTATED) {
				revokeFamily(current.getTokenFamilyId(), now, RevokeReason.REUSE_DETECTED, true);
				authSecurityEventLogger.logRefreshReuseDetected(current.getUser().getId(), current.getTokenFamilyId(), context);
				throw new BusinessException(ErrorCode.E409_TOKEN_REUSE_DETECTED, "리프레시 토큰 재사용이 감지되었습니다.");
			}
			throw new BusinessException(ErrorCode.E401_INVALID_TOKEN, "리프레시 토큰이 더 이상 활성 상태가 아닙니다.");
		}

		current.markUsed(now);
		current.revoke(RevokeReason.ROTATED, now);
		User user = current.getUser();
		if (!user.isActive()) {
			throw new BusinessException(ErrorCode.E403_FORBIDDEN, "비활성화된 사용자 계정입니다.");
		}

		String newRawRefreshToken = refreshTokenGenerator.generate();
		String newHash = refreshTokenHasher.hash(newRawRefreshToken);
		Instant refreshExpiresAt = now.plus(authProperties.getRefresh().getTtl());
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

		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole(), user.getStatus(), now);
		Instant accessExpiresAt = jwtTokenProvider.getAccessTokenExpiresAt(now);
		TokenPair result = new TokenPair(accessToken, accessExpiresAt, newRawRefreshToken, refreshExpiresAt);
		refreshRotationReplayStore.save(tokenHash, context, result, authProperties.getRefresh().getRotationReplayWindow());
		authSecurityEventLogger.logRefreshRotated(user.getId(), current.getTokenFamilyId(), context);
		return result;
	}

	@Override
	@Transactional
	public void revokeByRawToken(String presentedRefreshToken, RevokeReason reason) {
		String tokenHash = refreshTokenHasher.hash(presentedRefreshToken);
		refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
				.ifPresent(token -> token.revoke(reason, Instant.now()));
	}

	@Override
	@Transactional
	public void revokeAllByUserId(Long userId, RevokeReason reason) {
		Instant now = Instant.now();
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



