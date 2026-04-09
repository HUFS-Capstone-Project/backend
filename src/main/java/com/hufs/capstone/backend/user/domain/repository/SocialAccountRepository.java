package com.hufs.capstone.backend.user.domain.repository;

import com.hufs.capstone.backend.user.domain.entity.SocialAccount;
import com.hufs.capstone.backend.user.domain.enums.SocialProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

	Optional<SocialAccount> findByProviderAndProviderUserIdAndDeletedAtIsNull(
			SocialProvider provider,
			String providerUserId
	);
}



