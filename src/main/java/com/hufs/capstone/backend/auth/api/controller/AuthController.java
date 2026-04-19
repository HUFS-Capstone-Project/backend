package com.hufs.capstone.backend.auth.api.controller;

import com.hufs.capstone.backend.auth.api.controller.swagger.AuthCommonApi;
import com.hufs.capstone.backend.auth.api.controller.swagger.AuthMobileApi;
import com.hufs.capstone.backend.auth.api.controller.swagger.AuthWebApi;
import com.hufs.capstone.backend.auth.api.request.LogoutRequest;
import com.hufs.capstone.backend.auth.api.request.MobileExchangeRequest;
import com.hufs.capstone.backend.auth.api.request.RefreshRequest;
import com.hufs.capstone.backend.auth.api.request.WebExchangeTicketRequest;
import com.hufs.capstone.backend.auth.api.response.AuthTokenBootstrapResponse;
import com.hufs.capstone.backend.user.api.response.UserProfileResponse;
import com.hufs.capstone.backend.auth.api.response.TokenResponse;
import com.hufs.capstone.backend.auth.application.dto.TokenPair;
import com.hufs.capstone.backend.auth.application.dto.WebLoginTicketPayload;
import com.hufs.capstone.backend.auth.application.service.AuthLoginService;
import com.hufs.capstone.backend.auth.application.service.AuthQueryService;
import com.hufs.capstone.backend.auth.application.service.TokenLifecycleService;
import com.hufs.capstone.backend.auth.domain.enums.RevokeReason;
import com.hufs.capstone.backend.auth.domain.vo.ClientContext;
import com.hufs.capstone.backend.auth.infrastructure.web.CookieService;
import com.hufs.capstone.backend.auth.security.SecurityUtils;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.user.application.dto.UserProfileResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthCommonApi, AuthWebApi, AuthMobileApi {

	private final AuthLoginService authLoginService;
	private final AuthQueryService authQueryService;
	private final TokenLifecycleService tokenLifecycleService;
	private final CookieService cookieService;
	private final CsrfTokenRepository csrfTokenRepository;

	@Override
	public CommonResponse<AuthTokenBootstrapResponse> exchangeWebTicket(
			@Valid @RequestBody WebExchangeTicketRequest request
	) {
		WebLoginTicketPayload payload = authLoginService.exchangeWebTicket(request.ticket());
		UserProfileResult profile = authQueryService.getUserProfile(payload.userId());
		TokenResponse tokenResponse = TokenResponse.web(payload.accessToken(), payload.accessTokenExpiresAt());
		return CommonResponse.ok(new AuthTokenBootstrapResponse(tokenResponse, UserProfileResponse.from(profile)));
	}

	@Override
	public CommonResponse<TokenResponse> exchangeMobile(
			@Valid @RequestBody MobileExchangeRequest request,
			HttpServletRequest servletRequest,
			@RequestHeader(name = "X-Client-Platform", required = false) String clientPlatform
	) {
		ClientContext context = authLoginService.createAppClientContext(
				servletRequest.getHeader("User-Agent"),
				servletRequest.getRemoteAddr(),
				clientPlatform
		);
		TokenPair tokenPair = authLoginService.exchangeMobileCode(request.code(), request.codeVerifier(), context);
		TokenResponse response = new TokenResponse(
				tokenPair.accessToken(),
				tokenPair.accessTokenExpiresAt(),
				tokenPair.refreshToken(),
				tokenPair.refreshTokenExpiresAt()
		);
		return CommonResponse.ok(response);
	}

	@Override
	public CommonResponse<TokenResponse> refresh(
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		String refreshToken = cookieService.getRefreshToken(servletRequest)
				.orElseThrow(() -> new BusinessException(ErrorCode.E401_UNAUTHORIZED, "리프레시 토큰 쿠키가 필요합니다."));
		TokenPair rotated = tokenLifecycleService.rotate(
				refreshToken,
				authLoginService.createWebClientContext(servletRequest.getHeader("User-Agent"), servletRequest.getRemoteAddr())
		);
		cookieService.writeRefreshToken(servletResponse, rotated.refreshToken());
		return CommonResponse.ok(TokenResponse.web(rotated.accessToken(), rotated.accessTokenExpiresAt()));
	}

	@Override
	public CommonResponse<TokenResponse> mobileRefresh(
			@Valid @RequestBody RefreshRequest request,
			HttpServletRequest servletRequest,
			@RequestHeader(name = "X-Client-Platform", required = false) String clientPlatform
	) {
		if (!StringUtils.hasText(request.refreshToken())) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "리프레시 토큰이 필요합니다.");
		}
		TokenPair rotated = tokenLifecycleService.rotate(
				request.refreshToken(),
				authLoginService.createAppClientContext(servletRequest.getHeader("User-Agent"), servletRequest.getRemoteAddr(), clientPlatform)
		);
		TokenResponse response = new TokenResponse(
				rotated.accessToken(),
				rotated.accessTokenExpiresAt(),
				rotated.refreshToken(),
				rotated.refreshTokenExpiresAt()
		);
		return CommonResponse.ok(response);
	}

	@Override
	public ResponseEntity<Void> logout(
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse,
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		cookieService.getRefreshToken(servletRequest)
				.ifPresent(refreshToken -> tokenLifecycleService.revokeByRawToken(refreshToken, RevokeReason.LOGOUT));
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		new SecurityContextLogoutHandler().logout(servletRequest, servletResponse, authentication);
		cookieService.clearRefreshToken(servletResponse);
		rotateCsrfToken(servletRequest, servletResponse);
		return ResponseEntity.noContent()
				.cacheControl(CacheControl.noStore())
				.build();
	}

	@Override
	public CommonResponse<Void> mobileLogout(@Valid @RequestBody LogoutRequest request) {
		if (!StringUtils.hasText(request.refreshToken())) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "리프레시 토큰이 필요합니다.");
		}
		tokenLifecycleService.revokeByRawToken(request.refreshToken(), RevokeReason.LOGOUT);
		return CommonResponse.okMessage("로그아웃되었습니다.");
	}

	@Override
	public CommonResponse<Void> logoutAll(
			@RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfToken
	) {
		Long userId = SecurityUtils.currentUserIdOrThrow();
		tokenLifecycleService.revokeAllByUserId(userId, RevokeReason.ADMIN_FORCE);
		return CommonResponse.okMessage("모든 기기에서 로그아웃되었습니다.");
	}

	@Override
	public ResponseEntity<Void> csrf() {
		return ResponseEntity.noContent()
				.cacheControl(CacheControl.noStore())
				.build();
	}

	private void rotateCsrfToken(HttpServletRequest request, HttpServletResponse response) {
		csrfTokenRepository.saveToken(null, request, response);
		csrfTokenRepository.saveToken(csrfTokenRepository.generateToken(request), request, response);
	}
}



