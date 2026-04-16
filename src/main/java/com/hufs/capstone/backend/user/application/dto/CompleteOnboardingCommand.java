package com.hufs.capstone.backend.user.application.dto;

public record CompleteOnboardingCommand(
		String nickname,
		boolean serviceTermsAgreed,
		boolean privacyPolicyAgreed,
		boolean marketingNotificationAgreed
) {
}
