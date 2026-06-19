package com.hufs.capstone.backend.auth.application;

import com.hufs.capstone.backend.auth.application.dto.DevMasterTokenResult;
import com.hufs.capstone.backend.auth.application.dto.TokenPair;
import com.hufs.capstone.backend.auth.application.service.AuthLoginService;
import com.hufs.capstone.backend.auth.application.service.TokenLifecycleService;
import com.hufs.capstone.backend.auth.domain.vo.ClientContext;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.user.application.dto.UserProfileResult;
import com.hufs.capstone.backend.user.domain.entity.User;
import com.hufs.capstone.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.auth.dev-token", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class AuthDevTokenService {

	private final UserRepository userRepository;
	private final AuthLoginService authLoginService;
	private final TokenLifecycleService tokenLifecycleService;

	public DevMasterTokenResult issueMasterToken(Long userId, String userAgent, String ipAddress) {
		UserSelection userSelection = resolveUser(userId);
		User user = userSelection.user();
		if (!user.isActive()) {
			throw new BusinessException(ErrorCode.USER_ACCOUNT_DISABLED);
		}

		ClientContext context = authLoginService.createWebClientContext(userAgent, ipAddress);
		TokenPair tokenPair = tokenLifecycleService.issueInitial(user, context);
		return new DevMasterTokenResult(
				UserProfileResult.from(user),
				tokenPair,
				userSelection.createdUser()
		);
	}

	private UserSelection resolveUser(Long userId) {
		if (userId != null) {
			User found = userRepository.findByIdAndDeletedAtIsNull(userId)
					.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
			return new UserSelection(found, false);
		}

		User created = userRepository.save(
				User.register(
						buildDevEmail(),
						true,
						"swagger-dev-user",
						null
				)
		);
		return new UserSelection(created, true);
	}

	private static String buildDevEmail() {
		return "swagger-dev-" + Instant.now().toEpochMilli() + "@local.test";
	}

	private record UserSelection(User user, boolean createdUser) {
	}
}

