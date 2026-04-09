package com.hufs.capstone.backend.auth.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthCsrfIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

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
}
