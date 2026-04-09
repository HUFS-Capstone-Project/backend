package com.hufs.capstone.backend.auth.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

	private Jwt jwt = new Jwt();
	private Refresh refresh = new Refresh();
	private Cookie cookie = new Cookie();
	private Redirect redirect = new Redirect();
	private OneTimeCode oneTimeCode = new OneTimeCode();
	private Redis redis = new Redis();

	@Getter
	@Setter
	public static class Jwt {
		@NotBlank
		private String issuer = "udidura-backend";
		@NotBlank
		private String audience = "udidura-api";
		@NotBlank
		private String secretBase64 = "VGhpc0lzQVN0cm9uZ0RlbW9TZWNyZXRLZXlGb3JVZGlkdXJhQmFja2VuZA==";
		private Duration accessTokenTtl = Duration.ofMinutes(10);
	}

	@Getter
	@Setter
	public static class Refresh {
		private Duration ttl = Duration.ofDays(30);
		@NotBlank
		private String hashSecret = "udidura-refresh-hash-secret";
		@Min(16)
		private int tokenBytes = 32;
		private Duration rotationReplayWindow = Duration.ofSeconds(15);
		@Min(1)
		private int cleanupRetentionDays = 14;
	}

	@Getter
	@Setter
	public static class Cookie {
		@NotBlank
		private String refreshCookieName = "__Host-udidura_rt";
		@NotBlank
		private String oauthContextCookieName = "__Host-udidura_octx";
		private boolean secure = true;
		private boolean httpOnly = true;
		@NotBlank
		private String sameSite = "Lax";
		private String domain;
		@NotBlank
		private String refreshPath = "/api/v1/auth";
		@NotBlank
		private String oauthContextPath = "/login/oauth2/code/google";
		@Min(60)
		private int maxAgeSeconds = 2_592_000;
	}

	@Getter
	@Setter
	public static class Redirect {
		@NotBlank
		private String webBaseUrl = "https://web.example.com";
		@NotBlank
		private String defaultWebReturnPath = "/auth/callback";
		@NotEmpty
		private List<String> allowedWebReturnPaths = List.of("/auth/callback", "/app");
		@NotBlank
		private String appLinkBaseUrl = "https://app.example.com/auth/callback";
	}

	@Getter
	@Setter
	public static class OneTimeCode {
		private Duration ticketTtl = Duration.ofSeconds(60);
		private Duration mobileCodeTtl = Duration.ofSeconds(60);
		private Duration replayWindow = Duration.ofSeconds(20);
	}

	@Getter
	@Setter
	public static class Redis {
		@NotBlank
		private String keyPrefix = "auth";
	}
}



