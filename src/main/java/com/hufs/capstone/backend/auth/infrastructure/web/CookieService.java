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

	private final AuthProperties authProperties;

	public void writeRefreshToken(HttpServletResponse response, String refreshToken) {
		AuthProperties.Cookie cfg = authProperties.getCookie();
		ResponseCookie cookie = ResponseCookie.from(cfg.getRefreshCookieName(), refreshToken)
				.httpOnly(cfg.isHttpOnly())
				.secure(cfg.isSecure())
				.sameSite(cfg.getSameSite())
				.path(cfg.getRefreshPath())
				.maxAge(cfg.getMaxAgeSeconds())
				.domain(normalizeDomain(cfg.getDomain()))
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	public void clearRefreshToken(HttpServletResponse response) {
		AuthProperties.Cookie cfg = authProperties.getCookie();
		ResponseCookie cookie = ResponseCookie.from(cfg.getRefreshCookieName(), "")
				.httpOnly(cfg.isHttpOnly())
				.secure(cfg.isSecure())
				.sameSite(cfg.getSameSite())
				.path(cfg.getRefreshPath())
				.maxAge(0)
				.domain(normalizeDomain(cfg.getDomain()))
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	public Optional<String> getRefreshToken(HttpServletRequest request) {
		return getCookieValue(request, authProperties.getCookie().getRefreshCookieName());
	}

	public void writeOAuthContext(HttpServletResponse response, String encoded) {
		AuthProperties.Cookie cfg = authProperties.getCookie();
		ResponseCookie cookie = ResponseCookie.from(cfg.getOauthContextCookieName(), encoded)
				.httpOnly(true)
				.secure(cfg.isSecure())
				.sameSite(cfg.getSameSite())
				.path(cfg.getOauthContextPath())
				.maxAge(300)
				.domain(normalizeDomain(cfg.getDomain()))
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	public Optional<String> getOAuthContext(HttpServletRequest request) {
		return getCookieValue(request, authProperties.getCookie().getOauthContextCookieName());
	}

	public void clearOAuthContext(HttpServletResponse response) {
		AuthProperties.Cookie cfg = authProperties.getCookie();
		ResponseCookie cookie = ResponseCookie.from(cfg.getOauthContextCookieName(), "")
				.httpOnly(true)
				.secure(cfg.isSecure())
				.sameSite(cfg.getSameSite())
				.path(cfg.getOauthContextPath())
				.maxAge(0)
				.domain(normalizeDomain(cfg.getDomain()))
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
}



