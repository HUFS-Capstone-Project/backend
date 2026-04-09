package com.hufs.capstone.backend.auth.infrastructure.security;

import com.hufs.capstone.backend.auth.infrastructure.config.AuthProperties;
import com.hufs.capstone.backend.auth.security.AuthUserPrincipal;
import com.hufs.capstone.backend.user.domain.enums.UserRole;
import com.hufs.capstone.backend.user.domain.enums.UserStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private final AuthProperties authProperties;

	public String createAccessToken(Long userId, UserRole role, UserStatus status, Instant now) {
		Instant expiresAt = now.plus(authProperties.getJwt().getAccessTokenTtl());
		SecretKey key = signingKey();
		return Jwts.builder()
				.issuer(authProperties.getJwt().getIssuer())
				.audience().add(authProperties.getJwt().getAudience()).and()
				.subject(String.valueOf(userId))
				.id(UUID.randomUUID().toString())
				.claim("role", role.name())
				.claim("status", status.name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.signWith(key)
				.compact();
	}

	public Instant getAccessTokenExpiresAt(Instant now) {
		return now.plus(authProperties.getJwt().getAccessTokenTtl());
	}

	public Authentication getAuthentication(String token) {
		Claims claims = parseClaims(token);
		Long userId = Long.valueOf(claims.getSubject());
		UserRole role = UserRole.valueOf(claims.get("role", String.class));
		UserStatus status = UserStatus.valueOf(claims.get("status", String.class));
		AuthUserPrincipal principal = new AuthUserPrincipal(userId, role, status);
		Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
		return new UsernamePasswordAuthenticationToken(principal, token, authorities);
	}

	public boolean validateToken(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (ExpiredJwtException ex) {
			return false;
		} catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(signingKey())
				.requireIssuer(authProperties.getJwt().getIssuer())
				.requireAudience(authProperties.getJwt().getAudience())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private SecretKey signingKey() {
		byte[] keyBytes = Decoders.BASE64.decode(authProperties.getJwt().getSecretBase64());
		Key key = Keys.hmacShaKeyFor(keyBytes);
		if (key instanceof SecretKey secretKey) {
			return secretKey;
		}
		throw new IllegalStateException("JWT signing key is not a SecretKey.");
	}
}


