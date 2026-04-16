package com.hufs.capstone.backend.user.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.user.api.response.UserProfileResponse;
import com.hufs.capstone.backend.user.application.dto.CompleteOnboardingCommand;
import com.hufs.capstone.backend.user.domain.entity.User;
import com.hufs.capstone.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public UserProfileResponse getProfile(Long userId) {
		User user = getUserOrThrow(userId);
		return UserProfileResponse.from(user);
	}

	@Transactional
	public UserProfileResponse completeOnboarding(Long userId, CompleteOnboardingCommand command) {
		User user = getUserOrThrow(userId);
		if (user.isOnboardingCompleted()) {
			throw new BusinessException(ErrorCode.E409_CONFLICT, "이미 온보딩이 완료된 사용자입니다.");
		}

		user.completeOnboarding(
				command.nickname(),
				command.serviceTermsAgreed(),
				command.privacyPolicyAgreed(),
				command.marketingNotificationAgreed(),
				Instant.now()
		);
		return UserProfileResponse.from(user);
	}

	private User getUserOrThrow(Long userId) {
		return userRepository.findByIdAndDeletedAtIsNull(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "User not found."));
	}
}
