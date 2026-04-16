package com.hufs.capstone.backend.auth.application;

import com.hufs.capstone.backend.auth.api.response.DevMasterTokenResponse;
import com.hufs.capstone.backend.auth.api.response.MeResponse;
import com.hufs.capstone.backend.auth.api.response.TokenResponse;
import com.hufs.capstone.backend.auth.application.dto.TokenPair;
import com.hufs.capstone.backend.auth.application.service.AuthLoginService;
import com.hufs.capstone.backend.auth.application.service.TokenLifecycleService;
import com.hufs.capstone.backend.auth.domain.vo.ClientContext;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.user.domain.entity.User;
import com.hufs.capstone.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!prod")
@RequiredArgsConstructor
public class AuthDevTokenService {

	private final UserRepository userRepository;
	private final AuthLoginService authLoginService;
	private final TokenLifecycleService tokenLifecycleService;

	public DevMasterTokenResponse issueMasterToken(Long userId, String userAgent, String ipAddress) {
		UserSelection userSelection = resolveUser(userId);
		User user = userSelection.user();
		if (!user.isActive()) {
			throw new BusinessException(ErrorCode.E403_FORBIDDEN, "User account is not active.");
		}

		ClientContext context = authLoginService.createWebClientContext(userAgent, ipAddress);
		TokenPair tokenPair = tokenLifecycleService.issueInitial(user, context);
		return new DevMasterTokenResponse(
				MeResponse.from(user),
				new TokenResponse(
						tokenPair.accessToken(),
						tokenPair.accessTokenExpiresAt(),
						tokenPair.refreshToken(),
						tokenPair.refreshTokenExpiresAt()
				),
				userSelection.createdUser()
		);
	}

	private UserSelection resolveUser(Long userId) {
		if (userId != null) {
			User found = userRepository.findByIdAndDeletedAtIsNull(userId)
					.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "User not found."));
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
