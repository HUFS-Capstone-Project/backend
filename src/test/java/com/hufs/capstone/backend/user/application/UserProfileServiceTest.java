package com.hufs.capstone.backend.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.user.application.dto.CompleteOnboardingCommand;
import com.hufs.capstone.backend.user.application.dto.UserProfileResult;
import com.hufs.capstone.backend.user.domain.entity.User;
import com.hufs.capstone.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserProfileService userProfileService;

	@Test
	void completeOnboardingShouldStoreProfileAndAgreement() {
		User user = registeredUser(1L);
		when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

		UserProfileResult response = userProfileService.completeOnboarding(
				1L,
				new CompleteOnboardingCommand("nickname", true, true, false)
		);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.nickname()).isEqualTo("nickname");
		assertThat(response.onboardingCompleted()).isTrue();
		assertThat(user.getOnboardingCompletedAt()).isNotNull();
		assertThat(user.isServiceTermsAgreed()).isTrue();
		assertThat(user.isPrivacyPolicyAgreed()).isTrue();
		assertThat(user.isMarketingNotificationAgreed()).isFalse();
	}

	@Test
	void completeOnboardingShouldThrowConflictWhenAlreadyCompleted() {
		User user = registeredUser(1L);
		user.completeOnboarding("existing", true, true, true, Instant.parse("2026-04-16T00:00:00Z"));
		when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> userProfileService.completeOnboarding(
				1L,
				new CompleteOnboardingCommand("updated", true, true, false)
		))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E409_CONFLICT));

		assertThat(user.getNickname()).isEqualTo("existing");
		assertThat(user.getOnboardingCompletedAt()).isEqualTo(Instant.parse("2026-04-16T00:00:00Z"));
	}

	@Test
	void completeOnboardingShouldThrowWhenUserNotFound() {
		when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userProfileService.completeOnboarding(
				1L,
				new CompleteOnboardingCommand("nickname", true, true, false)
		))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E404_NOT_FOUND));
		verify(userRepository).findByIdAndDeletedAtIsNull(1L);
	}

	@Test
	void getProfileShouldReturnUserProfileResponse() {
		User user = registeredUser(1L);
		when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

		UserProfileResult response = userProfileService.getProfile(1L);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.email()).isEqualTo("test@example.com");
	}

	private static User registeredUser(Long id) {
		User user = User.register("test@example.com", true, "social", "https://image.example.com");
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
