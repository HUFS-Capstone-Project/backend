package com.hufs.capstone.backend.auth.domain.repository;

import com.hufs.capstone.backend.auth.domain.entity.RefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select rt from RefreshToken rt where rt.tokenHash = :tokenHash")
	Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	List<RefreshToken> findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(Long userId, Instant now);

	List<RefreshToken> findByTokenFamilyIdAndRevokedAtIsNull(UUID tokenFamilyId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			delete from RefreshToken rt
			where rt.expiresAt < :threshold
			and rt.revokedAt is not null
			""")
	int deleteRevokedExpiredBefore(@Param("threshold") Instant threshold);
}



