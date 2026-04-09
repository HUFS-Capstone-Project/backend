package com.hufs.capstone.backend.auth.oauth;

import com.hufs.capstone.backend.auth.domain.vo.AuthClientType;
import com.hufs.capstone.backend.auth.domain.vo.OAuthLoginContext;
import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import com.hufs.capstone.backend.auth.infrastructure.web.OAuthLoginContextCookieService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class OAuthAuthorizationContextCaptureFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION_ENDPOINT = "/oauth2/authorization/google";

	private final OAuthLoginContextCookieService oauthLoginContextCookieService;
	private final AuthProperties authProperties;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !AUTHORIZATION_ENDPOINT.equals(request.getRequestURI());
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String client = request.getParameter("client");
		AuthClientType clientType = "app".equalsIgnoreCase(client) ? AuthClientType.APP : AuthClientType.WEB;
		String returnPath = normalizeReturnPath(request.getParameter("return_to"));
		OAuthLoginContext context = new OAuthLoginContext(
				clientType,
				returnPath,
				request.getParameter("code_challenge"),
				request.getParameter("code_challenge_method")
		);
		oauthLoginContextCookieService.write(response, context);
		filterChain.doFilter(request, response);
	}

	private String normalizeReturnPath(String returnPath) {
		if (!StringUtils.hasText(returnPath) || returnPath.contains("://") || returnPath.startsWith("//")) {
			return authProperties.getRedirect().getDefaultWebReturnPath();
		}
		if (!authProperties.getRedirect().getAllowedWebReturnPaths().contains(returnPath)) {
			return authProperties.getRedirect().getDefaultWebReturnPath();
		}
		return returnPath;
	}
}



