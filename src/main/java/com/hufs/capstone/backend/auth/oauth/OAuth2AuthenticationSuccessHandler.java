package com.hufs.capstone.backend.auth.oauth;

import com.hufs.capstone.backend.auth.application.service.AuthLoginService;
import com.hufs.capstone.backend.auth.application.service.TokenLifecycleService;
import com.hufs.capstone.backend.auth.domain.vo.AuthClientType;
import com.hufs.capstone.backend.auth.domain.vo.ClientContext;
import com.hufs.capstone.backend.auth.domain.vo.OAuthLoginContext;
import com.hufs.capstone.backend.auth.application.dto.TokenPair;
import com.hufs.capstone.backend.auth.infrastructure.web.CookieService;
import com.hufs.capstone.backend.auth.infrastructure.web.DeepLinkRedirectService;
import com.hufs.capstone.backend.auth.infrastructure.web.OAuthLoginContextCookieService;
import com.hufs.capstone.backend.auth.infrastructure.web.WebRedirectService;
import com.hufs.capstone.backend.auth.oauth.userinfo.OAuth2UserInfoFactory;
import com.hufs.capstone.backend.user.domain.entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final OAuth2UserInfoFactory oauth2UserInfoFactory;
	private final AuthLoginService authLoginService;
	private final TokenLifecycleService tokenLifecycleService;
	private final OAuthLoginContextCookieService oauthLoginContextCookieService;
	private final CookieService cookieService;
	private final WebRedirectService webRedirectService;
	private final DeepLinkRedirectService deepLinkRedirectService;
	private final ObjectProvider<CsrfTokenRepository> csrfTokenRepositoryProvider;

	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication
	) throws IOException, ServletException {
		OAuthLoginContext context = oauthLoginContextCookieService.readOrDefault(request);
		oauthLoginContextCookieService.clear(response);
		try {
			OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
			String registrationId = "google";
			SocialIdentity identity = oauth2UserInfoFactory.from(registrationId, oidcUser);
			User user = authLoginService.upsertSocialUser(identity);
			rotateCsrfToken(request, response);
			if (context.clientType() == AuthClientType.APP) {
				handleAppLogin(request, response, context, user);
				return;
			}
			handleWebLogin(request, response, context, user);
		} catch (Exception ex) {
			log.error("OAuth2 success handler failed", ex);
			String redirect = context.clientType().isWeb()
					? webRedirectService.failure(context.returnPath(), "oauth_failed")
					: deepLinkRedirectService.failure("oauth_failed");
			getRedirectStrategy().sendRedirect(request, response, redirect);
		}
	}

	private void handleWebLogin(HttpServletRequest request, HttpServletResponse response, OAuthLoginContext context, User user)
			throws IOException {
		ClientContext clientContext = authLoginService.createWebClientContext(request.getHeader("User-Agent"), request.getRemoteAddr());
		TokenPair tokenPair = tokenLifecycleService.issueInitial(user, clientContext);
		cookieService.writeRefreshToken(response, tokenPair.refreshToken());
		String ticket = authLoginService.issueWebLoginTicket(user, tokenPair);
		String redirectUri = webRedirectService.success(context.returnPath(), ticket);
		getRedirectStrategy().sendRedirect(request, response, redirectUri);
	}

	private void handleAppLogin(HttpServletRequest request, HttpServletResponse response, OAuthLoginContext context, User user)
			throws IOException {
		String code = authLoginService.issueMobileAuthCode(user, context.codeChallenge(), context.codeChallengeMethod());
		String redirectUri = deepLinkRedirectService.success(code);
		getRedirectStrategy().sendRedirect(request, response, redirectUri);
	}

	private void rotateCsrfToken(HttpServletRequest request, HttpServletResponse response) {
		CsrfTokenRepository csrfTokenRepository = csrfTokenRepositoryProvider.getIfAvailable();
		if (csrfTokenRepository == null) {
			return;
		}
		csrfTokenRepository.saveToken(null, request, response);
		csrfTokenRepository.saveToken(csrfTokenRepository.generateToken(request), request, response);
	}
}


