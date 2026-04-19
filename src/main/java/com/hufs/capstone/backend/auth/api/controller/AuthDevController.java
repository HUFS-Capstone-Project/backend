package com.hufs.capstone.backend.auth.api.controller;

import com.hufs.capstone.backend.auth.api.controller.swagger.AuthDevApi;
import com.hufs.capstone.backend.auth.api.response.DevMasterTokenResponse;
import com.hufs.capstone.backend.auth.api.response.TokenResponse;
import com.hufs.capstone.backend.auth.application.AuthDevTokenService;
import com.hufs.capstone.backend.auth.application.dto.DevMasterTokenResult;
import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.user.api.response.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!prod")
@RequiredArgsConstructor
public class AuthDevController implements AuthDevApi {

	private final AuthDevTokenService authDevTokenService;

	@Override
	public CommonResponse<DevMasterTokenResponse> issueMasterToken(
			HttpServletRequest servletRequest,
			@RequestParam(name = "userId", required = false) Long userId
	) {
		DevMasterTokenResult result = authDevTokenService.issueMasterToken(
				userId,
				servletRequest.getHeader("User-Agent"),
				servletRequest.getRemoteAddr()
		);
		return CommonResponse.ok(
				new DevMasterTokenResponse(
						UserProfileResponse.from(result.profile()),
						new TokenResponse(
								result.tokenPair().accessToken(),
								result.tokenPair().accessTokenExpiresAt(),
								result.tokenPair().refreshToken(),
								result.tokenPair().refreshTokenExpiresAt()
						),
						result.createdUser()
				)
		);
	}
}
