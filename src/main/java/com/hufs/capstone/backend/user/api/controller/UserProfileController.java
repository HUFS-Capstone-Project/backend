package com.hufs.capstone.backend.user.api.controller;

import com.hufs.capstone.backend.auth.security.AuthUserPrincipal;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.user.api.controller.swagger.UserProfileApi;
import com.hufs.capstone.backend.user.api.request.CompleteOnboardingRequest;
import com.hufs.capstone.backend.user.api.request.UpdateNicknameRequest;
import com.hufs.capstone.backend.user.api.response.UserProfileResponse;
import com.hufs.capstone.backend.user.application.UserProfileService;
import com.hufs.capstone.backend.user.application.dto.CompleteOnboardingCommand;
import com.hufs.capstone.backend.user.application.dto.UpdateNicknameCommand;
import com.hufs.capstone.backend.user.application.dto.UserProfileResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserProfileController implements UserProfileApi {

	private final UserProfileService userProfileService;

	@Override
	public CommonResponse<UserProfileResponse> getProfile(@AuthenticationPrincipal AuthUserPrincipal principal) {
		UserProfileResult result = userProfileService.getProfile(extractUserId(principal));
		return CommonResponse.ok(UserProfileResponse.from(result));
	}

	@Override
	public CommonResponse<UserProfileResponse> completeOnboarding(
			@Valid @RequestBody CompleteOnboardingRequest request,
			String csrfToken,
			@AuthenticationPrincipal AuthUserPrincipal principal
	) {
		UserProfileResult result = userProfileService.completeOnboarding(
				extractUserId(principal),
				new CompleteOnboardingCommand(
						request.nickname(),
						request.serviceTermsAgreed(),
						request.privacyPolicyAgreed(),
						request.marketingNotificationAgreed()
				)
		);
		return CommonResponse.ok(UserProfileResponse.from(result));
	}

	@Override
	public CommonResponse<UserProfileResponse> updateNickname(
			@Valid @RequestBody UpdateNicknameRequest request,
			String csrfToken,
			@AuthenticationPrincipal AuthUserPrincipal principal
	) {
		UserProfileResult result = userProfileService.updateNickname(
				extractUserId(principal),
				new UpdateNicknameCommand(request.nickname())
		);
		return CommonResponse.ok(UserProfileResponse.from(result));
	}

	private static Long extractUserId(AuthUserPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(ErrorCode.AUTHENTICATED_USER_NOT_FOUND);
		}
		return principal.userId();
	}
}

