package com.hufs.capstone.backend.auth.application.service;

import com.hufs.capstone.backend.auth.oauth.SocialIdentity;
import com.hufs.capstone.backend.auth.domain.vo.ClientContext;
import com.hufs.capstone.backend.auth.application.dto.TokenPair;
import com.hufs.capstone.backend.auth.application.dto.WebLoginTicketPayload;
import com.hufs.capstone.backend.user.domain.entity.User;

public interface AuthLoginService {

	User upsertSocialUser(SocialIdentity socialIdentity);

	String issueWebLoginTicket(User user, TokenPair tokenPair);

	String issueMobileAuthCode(User user, String codeChallenge, String codeChallengeMethod);

	WebLoginTicketPayload exchangeWebTicket(String ticket);

	TokenPair exchangeMobileCode(String code, String codeVerifier, ClientContext context);

	ClientContext createWebClientContext(String userAgent, String ipAddress);

	ClientContext createAppClientContext(String userAgent, String ipAddress, String platformHeader);
}


