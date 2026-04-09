package com.hufs.capstone.backend.global.config;

import com.hufs.capstone.backend.auth.oauth.CustomOidcUserService;
import com.hufs.capstone.backend.auth.oauth.OAuth2AuthenticationFailureHandler;
import com.hufs.capstone.backend.auth.oauth.OAuth2AuthenticationSuccessHandler;
import com.hufs.capstone.backend.auth.oauth.OAuthAuthorizationContextCaptureFilter;
import com.hufs.capstone.backend.auth.security.JwtAuthenticationFilter;
import com.hufs.capstone.backend.auth.security.RestAccessDeniedHandler;
import com.hufs.capstone.backend.auth.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final OAuthAuthorizationContextCaptureFilter oauthAuthorizationContextCaptureFilter;
	private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
	private final OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;
	private final CustomOidcUserService customOidcUserService;
	private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
	private final RestAccessDeniedHandler restAccessDeniedHandler;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						.ignoringRequestMatchers(
								"/oauth2/**",
								"/login/oauth2/**",
								"/api/v1/auth/web/exchange-ticket",
								"/api/v1/auth/mobile/exchange",
								"/api/v1/auth/mobile/refresh",
								"/api/v1/auth/mobile/logout"
						)
				)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable())
				.oauth2Login(oauth2 -> oauth2
						.userInfoEndpoint(endpoint -> endpoint.oidcUserService(customOidcUserService))
						.successHandler(oauth2AuthenticationSuccessHandler)
						.failureHandler(oauth2AuthenticationFailureHandler)
				)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/v1/health",
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs/**",
								"/actuator/**",
								"/oauth2/**",
								"/login/oauth2/**",
								"/api/v1/auth/web/exchange-ticket",
								"/api/v1/auth/mobile/exchange",
								"/api/v1/auth/mobile/refresh",
								"/api/v1/auth/mobile/logout",
								"/api/v1/auth/csrf",
								"/api/v1/auth/refresh",
								"/api/v1/auth/logout"
						).permitAll()
						.anyRequest().authenticated()
				)
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(restAuthenticationEntryPoint)
						.accessDeniedHandler(restAccessDeniedHandler)
				)
				.addFilterBefore(oauthAuthorizationContextCaptureFilter, OAuth2AuthorizationRequestRedirectFilter.class)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.oauth2Client(Customizer.withDefaults());

		return http.build();
	}
}


