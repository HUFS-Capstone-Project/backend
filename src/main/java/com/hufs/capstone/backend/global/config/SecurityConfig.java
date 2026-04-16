package com.hufs.capstone.backend.global.config;

import com.hufs.capstone.backend.auth.oauth.CustomOidcUserService;
import com.hufs.capstone.backend.auth.oauth.OAuth2AuthenticationFailureHandler;
import com.hufs.capstone.backend.auth.oauth.OAuth2AuthenticationSuccessHandler;
import com.hufs.capstone.backend.auth.oauth.OAuthAuthorizationContextCaptureFilter;
import com.hufs.capstone.backend.auth.security.JwtAuthenticationFilter;
import com.hufs.capstone.backend.auth.security.RestAccessDeniedHandler;
import com.hufs.capstone.backend.auth.security.RestAuthenticationEntryPoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
	private final Environment environment;

	@Bean
	public CookieCsrfTokenRepository cookieCsrfTokenRepository(
			@Value("${app.security.csrf.cookie.same-site:Lax}") String sameSite,
			@Value("${app.security.csrf.cookie.secure:true}") boolean secure,
			@Value("${app.security.csrf.cookie.path:/}") String path,
			@Value("${app.security.csrf.cookie.domain:}") String domain) {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(builder -> {
			builder.sameSite(sameSite);
			builder.secure(secure);
			builder.path(path);
			if (StringUtils.hasText(domain)) {
				builder.domain(domain);
			}
		});
		return repository;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource,
			CookieCsrfTokenRepository cookieCsrfTokenRepository)
			throws Exception {
		boolean swaggerExposed = !Arrays.asList(environment.getActiveProfiles()).contains("prod");
		SpaCsrfTokenRequestHandler spaCsrfTokenRequestHandler = new SpaCsrfTokenRequestHandler();
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(csrf -> csrf
						.csrfTokenRepository(cookieCsrfTokenRepository)
						.csrfTokenRequestHandler(spaCsrfTokenRequestHandler)
						.ignoringRequestMatchers(
								"/oauth2/**",
								"/login/oauth2/**",
								"/api/v1/auth/web/exchange-ticket",
								"/api/v1/auth/mobile/exchange",
								"/api/v1/auth/mobile/refresh",
								"/api/v1/auth/mobile/logout",
								"/api/v1/auth/dev/**"
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
				.authorizeHttpRequests(auth -> {
					auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
					if (swaggerExposed) {
						auth.requestMatchers(
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs/**",
								"/api/v1/auth/dev/**"
						).permitAll();
					}
					auth.requestMatchers(
							"/api/v1/health",
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
					).permitAll();
					auth.anyRequest().authenticated();
				})
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(restAuthenticationEntryPoint)
						.accessDeniedHandler(restAccessDeniedHandler)
				)
				.addFilterBefore(oauthAuthorizationContextCaptureFilter, OAuth2AuthorizationRequestRedirectFilter.class)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.oauth2Client(Customizer.withDefaults());

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.setAllowedOrigins(corsProperties.getAllowedOrigins());
		corsConfiguration.setAllowedMethods(corsProperties.getAllowedMethods());
		corsConfiguration.setAllowedHeaders(corsProperties.getAllowedHeaders());
		corsConfiguration.setExposedHeaders(corsProperties.getExposedHeaders());
		corsConfiguration.setAllowCredentials(corsProperties.isAllowCredentials());
		corsConfiguration.setMaxAge(corsProperties.getMaxAge());

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", corsConfiguration);
		source.registerCorsConfiguration("/oauth2/**", corsConfiguration);
		source.registerCorsConfiguration("/login/oauth2/**", corsConfiguration);
		return source;
	}

	static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

		private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
		private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

		@Override
		public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
			this.xor.handle(request, response, csrfToken);
			csrfToken.get();
		}

		@Override
		public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
			String csrfHeaderValue = request.getHeader(csrfToken.getHeaderName());
			if (StringUtils.hasText(csrfHeaderValue)) {
				return this.plain.resolveCsrfTokenValue(request, csrfToken);
			}
			return this.xor.resolveCsrfTokenValue(request, csrfToken);
		}
	}
}


