package com.hufs.capstone.backend.user.api.controller;

import com.hufs.capstone.backend.auth.security.AuthUserPrincipal;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.global.response.CommonResponse;
import com.hufs.capstone.backend.user.api.controller.swagger.UserProfileApi;
import com.hufs.capstone.backend.user.api.request.CompleteOnboardingRequest;
import com.hufs.capstone.backend.user.api.response.UserProfileResponse;
import com.hufs.capstone.backend.user.application.UserProfileService;
import com.hufs.capstone.backend.user.application.dto.CompleteOnboardingCommand;
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
		return CommonResponse.ok(userProfileService.getProfile(extractUserId(principal)));
	}

	@Override
	public CommonResponse<UserProfileResponse> completeOnboarding(
			@Valid @RequestBody CompleteOnboardingRequest request,
			String csrfToken,
			@AuthenticationPrincipal AuthUserPrincipal principal
	) {
		UserProfileResponse response = userProfileService.completeOnboarding(
				extractUserId(principal),
				new CompleteOnboardingCommand(
						request.nickname(),
						request.serviceTermsAgreed(),
						request.privacyPolicyAgreed(),
						request.marketingNotificationAgreed()
				)
		);
		return CommonResponse.ok(response);
	}

	private static Long extractUserId(AuthUserPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(ErrorCode.E401_UNAUTHORIZED, "Authenticated user not found.");
		}
		return principal.userId();
	}
}
