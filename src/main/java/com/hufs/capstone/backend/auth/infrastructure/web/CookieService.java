package com.hufs.capstone.backend.auth.infrastructure.web;

import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CookieService {

	private static final String HOST_PREFIX = "__Host-";
	private static final String ROOT_PATH = "/";

	private final AuthProperties authProperties;

	public void writeRefreshToken(HttpServletResponse response, String refreshToken) {
		AuthProperties.Cookie cfg = authProperties.getCookie();
		ResponseCookie cookie = buildCookie(cfg.getRefreshCookieName(), refreshToken, cfg.isHttpOnly(), cfg.getRefreshPath())
				.maxAge(cfg.getMaxAgeSeconds())
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	public void clearRefreshToken(HttpServletResponse response) {
		AuthProperties.Cookie cfg = authProperties.getCookie();
		ResponseCookie cookie = buildCookie(cfg.getRefreshCookieName(), "", cfg.isHttpOnly(), cfg.getRefreshPath())
				.maxAge(0)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	public Optional<String> getRefreshToken(HttpServletRequest request) {
		return getCookieValue(request, authProperties.getCookie().getRefreshCookieName());
	}

	public void writeOAuthContext(HttpServletResponse response, String encoded) {
		AuthProperties.Cookie cfg = authProperties.getCookie();
		ResponseCookie cookie = buildCookie(cfg.getOauthContextCookieName(), encoded, true, cfg.getOauthContextPath())
				.maxAge(300)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	public Optional<String> getOAuthContext(HttpServletRequest request) {
		return getCookieValue(request, authProperties.getCookie().getOauthContextCookieName());
	}

	public void clearOAuthContext(HttpServletResponse response) {
		AuthProperties.Cookie cfg = authProperties.getCookie();
		ResponseCookie cookie = buildCookie(cfg.getOauthContextCookieName(), "", true, cfg.getOauthContextPath())
				.maxAge(0)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	public String encode(String raw) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	public String decode(String value) {
		return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
	}

	private Optional<String> getCookieValue(HttpServletRequest request, String cookieName) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return Arrays.stream(cookies)
				.filter(cookie -> cookieName.equals(cookie.getName()))
				.map(Cookie::getValue)
				.filter(StringUtils::hasText)
				.findFirst();
	}

	private String normalizeDomain(String domain) {
		if (!StringUtils.hasText(domain)) {
			return null;
		}
		return domain;
	}

	private ResponseCookie.ResponseCookieBuilder buildCookie(
			String cookieName,
			String value,
			boolean httpOnly,
			String configuredPath
	) {
		AuthProperties.Cookie cfg = authProperties.getCookie();
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, value)
				.httpOnly(httpOnly)
				.secure(resolveSecure(cookieName, cfg.isSecure()))
				.sameSite(cfg.getSameSite())
				.path(resolvePath(cookieName, configuredPath));
		String domain = resolveDomain(cookieName, cfg.getDomain());
		if (domain != null) {
			builder.domain(domain);
		}
		return builder;
	}

	private String resolvePath(String cookieName, String configuredPath) {
		if (isHostPrefixCookie(cookieName)) {
			return ROOT_PATH;
		}
		if (!StringUtils.hasText(configuredPath)) {
			return ROOT_PATH;
		}
		return configuredPath;
	}

	private String resolveDomain(String cookieName, String configuredDomain) {
		if (isHostPrefixCookie(cookieName)) {
			return null;
		}
		return normalizeDomain(configuredDomain);
	}

	private boolean resolveSecure(String cookieName, boolean configuredSecure) {
		if (isHostPrefixCookie(cookieName)) {
			return true;
		}
		return configuredSecure;
	}

	private boolean isHostPrefixCookie(String cookieName) {
		return cookieName != null && cookieName.startsWith(HOST_PREFIX);
	}
}



