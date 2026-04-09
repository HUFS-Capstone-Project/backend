package com.hufs.capstone.backend.user.application;

import com.hufs.capstone.backend.auth.oauth.SocialIdentity;
import com.hufs.capstone.backend.user.domain.entity.SocialAccount;
import com.hufs.capstone.backend.user.domain.entity.User;
import com.hufs.capstone.backend.user.domain.repository.SocialAccountRepository;
import com.hufs.capstone.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSocialAccountService {

	private final UserRepository userRepository;
	private final SocialAccountRepository socialAccountRepository;

	@Transactional
	public User getOrCreateBySocialIdentity(SocialIdentity identity) {
		return socialAccountRepository.findByProviderAndProviderUserIdAndDeletedAtIsNull(identity.provider(), identity.providerUserId())
				.map(account -> {
					Instant now = Instant.now();
					account.markLogin(now);
					account.updateProviderProfile(identity.email(), identity.emailVerified());
					User linkedUser = account.getUser();
					linkedUser.markLoginSuccess(now);
					return linkedUser;
				})
				.orElseGet(() -> createUserAndSocialAccount(identity));
	}

	private User createUserAndSocialAccount(SocialIdentity identity) {
		User created = userRepository.save(User.register(identity.email(), identity.emailVerified(), identity.nickname()));
		socialAccountRepository.save(
				SocialAccount.link(created, identity.provider(), identity.providerUserId(), identity.email(), identity.emailVerified())
		);
		return created;
	}
}



