package com.hufs.capstone.backend.auth.oauth;

import com.hufs.capstone.backend.auth.domain.vo.OAuthLoginContext;
import com.hufs.capstone.backend.auth.infrastructure.web.DeepLinkRedirectService;
import com.hufs.capstone.backend.auth.infrastructure.web.OAuthLoginContextCookieService;
import com.hufs.capstone.backend.auth.infrastructure.web.WebRedirectService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	private final OAuthLoginContextCookieService oauthLoginContextCookieService;
	private final WebRedirectService webRedirectService;
	private final DeepLinkRedirectService deepLinkRedirectService;

	@Override
	public void onAuthenticationFailure(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception
	) throws IOException, ServletException {
		OAuthLoginContext context = oauthLoginContextCookieService.readOrDefault(request);
		oauthLoginContextCookieService.clear(response);
		String redirectUri = context.clientType().isWeb()
				? webRedirectService.failure(context.returnPath(), "oauth_failed")
				: deepLinkRedirectService.failure("oauth_failed");
		getRedirectStrategy().sendRedirect(request, response, redirectUri);
	}
}



