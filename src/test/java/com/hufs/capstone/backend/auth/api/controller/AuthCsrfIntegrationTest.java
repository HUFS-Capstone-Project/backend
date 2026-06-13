package com.hufs.capstone.backend.auth.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hufs.capstone.backend.auth.infrastructure.security.JwtTokenProvider;
import com.hufs.capstone.backend.user.domain.entity.User;
import com.hufs.capstone.backend.user.domain.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthCsrfIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void csrfBootstrapEndpointReturns204AndIssuesXsrfCookie() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
				.andExpect(status().isNoContent())
				.andReturn();

		String setCookieHeaders = String.join(",", result.getResponse().getHeaders(HttpHeaders.SET_COOKIE));
		assertThat(setCookieHeaders).contains("XSRF-TOKEN=");
		assertThat(setCookieHeaders).containsIgnoringCase("Path=/");
	}

	@Test
	void logoutReturns403WhenCsrfHeaderDoesNotMatchCookie() throws Exception {
		MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
				.andExpect(status().isNoContent())
				.andReturn();

		Cookie xsrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
		assertThat(xsrfCookie).isNotNull();

		mockMvc.perform(post("/api/v1/auth/logout")
						.cookie(xsrfCookie)
						.header("X-XSRF-TOKEN", "mismatch-token"))
				.andExpect(status().isForbidden());
	}

	@Test
	void logoutReturns204EvenWithoutRefreshTokenWhenCsrfMatches() throws Exception {
		MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
				.andExpect(status().isNoContent())
				.andReturn();

		Cookie xsrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
		assertThat(xsrfCookie).isNotNull();

		mockMvc.perform(post("/api/v1/auth/logout")
						.cookie(xsrfCookie)
						.header("X-XSRF-TOKEN", xsrfCookie.getValue()))
				.andExpect(status().isNoContent());
	}

	@Test
	void logoutReturns204EvenWhenAuthorizationBearerIsInvalid() throws Exception {
		MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
				.andExpect(status().isNoContent())
				.andReturn();

		Cookie xsrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
		assertThat(xsrfCookie).isNotNull();

		mockMvc.perform(post("/api/v1/auth/logout")
						.cookie(xsrfCookie)
						.header("X-XSRF-TOKEN", xsrfCookie.getValue())
						.header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token"))
				.andExpect(status().isNoContent());
	}

	@Test
	void logoutWithTrailingSlashReturns401WhenNoAuth() throws Exception {
		MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
				.andExpect(status().isNoContent())
				.andReturn();

		Cookie xsrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
		assertThat(xsrfCookie).isNotNull();

		mockMvc.perform(post("/api/v1/auth/logout/")
						.cookie(xsrfCookie)
						.header("X-XSRF-TOKEN", xsrfCookie.getValue()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void onboardingWithBearerTokenDoesNotRequireCsrfToken() throws Exception {
		User user = userRepository.save(User.register("mobile-csrf@example.com", true, "social", "https://image.example.com"));
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole(), user.getStatus(), Instant.now());

		mockMvc.perform(post("/api/v1/users/me/onboarding")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.header("X-Client-Platform", "android")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nickname": "mobile",
								  "serviceTermsAgreed": true,
								  "privacyPolicyAgreed": true,
								  "marketingNotificationAgreed": false
								}
								"""))
				.andExpect(status().isOk());
	}

	@Test
	void webRefreshStillRequiresCsrfTokenEvenWithBearerToken() throws Exception {
		User user = userRepository.save(User.register("web-refresh-csrf@example.com", true, "social", "https://image.example.com"));
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole(), user.getStatus(), Instant.now());

		mockMvc.perform(post("/api/v1/auth/refresh")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}
}
