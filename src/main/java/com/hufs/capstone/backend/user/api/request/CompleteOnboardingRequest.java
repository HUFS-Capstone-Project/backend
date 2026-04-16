package com.hufs.capstone.backend.user.api.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteOnboardingRequest(
		@NotBlank(message = "닉네임은 필수입니다.")
		@Size(max = 10, message = "닉네임은 최대 10자까지 가능합니다.")
		String nickname,

		@AssertTrue(message = "서비스 이용약관 동의는 필수입니다.")
		boolean serviceTermsAgreed,

		@AssertTrue(message = "개인정보 수집 및 이용 동의는 필수입니다.")
		boolean privacyPolicyAgreed,

		boolean marketingNotificationAgreed
) {

	public CompleteOnboardingRequest {
		if (nickname != null) {
			nickname = nickname.trim();
		}
	}
}
