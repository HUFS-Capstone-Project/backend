package com.hufs.capstone.backend.auth.application.service.impl;

import com.hufs.capstone.backend.auth.application.service.AuthLoginService;
import com.hufs.capstone.backend.auth.application.service.OneTimeCodeService;
import com.hufs.capstone.backend.auth.application.service.TokenLifecycleService;
import com.hufs.capstone.backend.auth.domain.enums.ClientPlatform;
import com.hufs.capstone.backend.auth.domain.enums.DeviceType;
import com.hufs.capstone.backend.auth.domain.vo.ClientContext;
import com.hufs.capstone.backend.auth.application.dto.MobileAuthCodePayload;
import com.hufs.capstone.backend.auth.application.dto.TokenPair;
import com.hufs.capstone.backend.auth.application.dto.WebLoginTicketPayload;
import com.hufs.capstone.backend.auth.oauth.SocialIdentity;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.user.application.UserSocialAccountService;
import com.hufs.capstone.backend.user.domain.entity.User;
import com.hufs.capstone.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthLoginServiceImpl implements AuthLoginService {

	private final UserSocialAccountService userSocialAccountService;
	private final UserRepository userRepository;
	private final TokenLifecycleService tokenLifecycleService;
	private final OneTimeCodeService oneTimeCodeService;
	private final PkceService pkceService;

	@Override
	@Transactional
	public User upsertSocialUser(SocialIdentity socialIdentity) {
		User user = userSocialAccountService.getOrCreateBySocialIdentity(socialIdentity);
		if (!user.isActive()) {
			throw new BusinessException(ErrorCode.E403_FORBIDDEN, "User account is not active.");
		}
		return user;
	}

	@Override
	@Transactional(readOnly = true)
	public String issueWebLoginTicket(User user, TokenPair tokenPair) {
		WebLoginTicketPayload payload = new WebLoginTicketPayload(
				user.getId(),
				tokenPair.accessToken(),
				tokenPair.accessTokenExpiresAt(),
				Instant.now()
		);
		return oneTimeCodeService.issueWebLoginTicket(payload);
	}

	@Override
	@Transactional
	public String issueMobileAuthCode(User user, String codeChallenge, String codeChallengeMethod) {
		if (!StringUtils.hasText(codeChallenge) || !"S256".equalsIgnoreCase(codeChallengeMethod)) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "Mobile login requires PKCE(S256).");
		}
		MobileAuthCodePayload payload = new MobileAuthCodePayload(user.getId(), codeChallenge, codeChallengeMethod, Instant.now());
		return oneTimeCodeService.issueMobileAuthCode(payload);
	}

	@Override
	@Transactional(readOnly = true)
	public WebLoginTicketPayload exchangeWebTicket(String ticket) {
		return oneTimeCodeService.consumeWebLoginTicket(ticket);
	}

	@Override
	@Transactional
	public TokenPair exchangeMobileCode(String code, String codeVerifier, ClientContext context) {
		MobileAuthCodePayload payload = oneTimeCodeService.consumeMobileAuthCode(code);
		pkceService.verify(payload.codeChallenge(), payload.codeChallengeMethod(), codeVerifier);
		User user = userRepository.findByIdAndDeletedAtIsNull(payload.userId())
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "User not found."));
		return tokenLifecycleService.issueInitial(user, context);
	}

	@Override
	public ClientContext createWebClientContext(String userAgent, String ipAddress) {
		return new ClientContext(DeviceType.WEB, ClientPlatform.REACT_WEB, userAgent, ipAddress);
	}

	@Override
	public ClientContext createAppClientContext(String userAgent, String ipAddress, String platformHeader) {
		if ("ios".equalsIgnoreCase(platformHeader)) {
			return new ClientContext(DeviceType.IOS, ClientPlatform.CAPACITOR_IOS, userAgent, ipAddress);
		}
		return new ClientContext(DeviceType.ANDROID, ClientPlatform.CAPACITOR_ANDROID, userAgent, ipAddress);
	}
}
