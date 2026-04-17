package com.hufs.capstone.backend.auth.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

class CookieServiceTest {

	@Test
	void hostPrefixRefreshCookieForcesSecureRootPathAndNoDomain() {
		AuthProperties properties = new AuthProperties();
		properties.getCookie().setRefreshCookieName("__Host-udidura_rt");
		properties.getCookie().setSecure(false);
		properties.getCookie().setDomain("udidura.vercel.app");
		properties.getCookie().setRefreshPath("/api/v1/auth");
		CookieService cookieService = new CookieService(properties);
		MockHttpServletResponse response = new MockHttpServletResponse();

		cookieService.writeRefreshToken(response, "refresh-token");

		String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
		assertThat(setCookie).contains("__Host-udidura_rt=refresh-token");
		assertThat(setCookie).containsIgnoringCase("Path=/");
		assertThat(setCookie).containsIgnoringCase("Secure");
		assertThat(setCookie).containsIgnoringCase("HttpOnly");
		assertThat(setCookie).doesNotContain("Domain=");
	}

	@Test
	void hostPrefixOauthContextCookieForcesSecureRootPathAndNoDomain() {
		AuthProperties properties = new AuthProperties();
		properties.getCookie().setOauthContextCookieName("__Host-udidura_octx");
		properties.getCookie().setSecure(false);
		properties.getCookie().setDomain("udidura.vercel.app");
		properties.getCookie().setOauthContextPath("/login/oauth2/code/google");
		CookieService cookieService = new CookieService(properties);
		MockHttpServletResponse response = new MockHttpServletResponse();

		cookieService.writeOAuthContext(response, "encoded-context");

		String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
		assertThat(setCookie).contains("__Host-udidura_octx=encoded-context");
		assertThat(setCookie).containsIgnoringCase("Path=/");
		assertThat(setCookie).containsIgnoringCase("Secure");
		assertThat(setCookie).containsIgnoringCase("HttpOnly");
		assertThat(setCookie).doesNotContain("Domain=");
	}

	@Test
	void nonHostRefreshCookieKeepsConfiguredPathAndDomain() {
		AuthProperties properties = new AuthProperties();
		properties.getCookie().setRefreshCookieName("udidura_rt");
		properties.getCookie().setSecure(false);
		properties.getCookie().setDomain("udidura.vercel.app");
		properties.getCookie().setRefreshPath("/api/v1/auth");
		CookieService cookieService = new CookieService(properties);
		MockHttpServletResponse response = new MockHttpServletResponse();

		cookieService.writeRefreshToken(response, "refresh-token");

		String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
		assertThat(setCookie).contains("udidura_rt=refresh-token");
		assertThat(setCookie).containsIgnoringCase("Path=/api/v1/auth");
		assertThat(setCookie).contains("Domain=udidura.vercel.app");
		assertThat(setCookie).doesNotContain("Secure");
	}
}

