package com.hufs.capstone.backend.auth.infrastructure.web;

import com.hufs.capstone.backend.auth.domain.vo.AuthClientType;
import com.hufs.capstone.backend.auth.domain.vo.OAuthLoginContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OAuthLoginContextCookieService {

	private final CookieService cookieService;

	public void write(HttpServletResponse response, OAuthLoginContext context) {
		String raw = serialize(context);
		cookieService.writeOAuthContext(response, cookieService.encode(raw));
	}

	public OAuthLoginContext readOrDefault(HttpServletRequest request) {
		return cookieService.getOAuthContext(request)
				.map(cookieService::decode)
				.map(this::deserialize)
				.orElseGet(OAuthLoginContext::webDefault);
	}

	public void clear(HttpServletResponse response) {
		cookieService.clearOAuthContext(response);
	}

	private String serialize(OAuthLoginContext context) {
		return "client=" + encode(context.clientType().name())
				+ "&returnPath=" + encode(context.returnPath())
				+ "&codeChallenge=" + encode(nullToEmpty(context.codeChallenge()))
				+ "&codeChallengeMethod=" + encode(nullToEmpty(context.codeChallengeMethod()));
	}

	private OAuthLoginContext deserialize(String raw) {
		Map<String, String> map = Arrays.stream(raw.split("&"))
				.map(item -> item.split("=", 2))
				.filter(parts -> parts.length == 2)
				.collect(Collectors.toMap(parts -> decode(parts[0]), parts -> decode(parts[1]), (a, b) -> b));

		String clientValue = map.getOrDefault("client", AuthClientType.WEB.name());
		AuthClientType clientType = parseClientType(clientValue);
		String returnPath = map.getOrDefault("returnPath", "/auth/callback");
		String codeChallenge = emptyToNull(map.get("codeChallenge"));
		String codeChallengeMethod = emptyToNull(map.get("codeChallengeMethod"));
		return new OAuthLoginContext(clientType, returnPath, codeChallenge, codeChallengeMethod);
	}

	private AuthClientType parseClientType(String value) {
		try {
			return AuthClientType.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException ex) {
			return AuthClientType.WEB;
		}
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private String emptyToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value;
	}
}



