package com.hufs.capstone.backend.auth.application.service.impl;

import com.hufs.capstone.backend.auth.application.ExpiringCodeStore;
import com.hufs.capstone.backend.auth.application.service.AuthSecurityEventLogger;
import com.hufs.capstone.backend.auth.application.service.OneTimeCodeService;
import com.hufs.capstone.backend.auth.application.dto.MobileAuthCodePayload;
import com.hufs.capstone.backend.auth.application.dto.WebLoginTicketPayload;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class OneTimeCodeServiceImpl implements OneTimeCodeService {

	private final ExpiringCodeStore<WebLoginTicketPayload> webTicketStore;
	private final ExpiringCodeStore<MobileAuthCodePayload> mobileCodeStore;
	private final AuthSecurityEventLogger authSecurityEventLogger;

	public OneTimeCodeServiceImpl(
			@Qualifier("webLoginTicketStore") ExpiringCodeStore<WebLoginTicketPayload> webTicketStore,
			@Qualifier("mobileAuthCodeStore") ExpiringCodeStore<MobileAuthCodePayload> mobileCodeStore,
			AuthSecurityEventLogger authSecurityEventLogger
	) {
		this.webTicketStore = webTicketStore;
		this.mobileCodeStore = mobileCodeStore;
		this.authSecurityEventLogger = authSecurityEventLogger;
	}

	@Override
	public String issueWebLoginTicket(WebLoginTicketPayload payload) {
		return webTicketStore.issue(payload);
	}

	@Override
	public WebLoginTicketPayload consumeWebLoginTicket(String ticket) {
		WebLoginTicketPayload payload = webTicketStore.consume(ticket);
		if (payload == null) {
			throw new BusinessException(ErrorCode.E401_INVALID_TOKEN, "로그인 티켓이 유효하지 않거나 만료되었습니다.");
		}
		authSecurityEventLogger.logOneTimeCodeConsumed("WEB_LOGIN_TICKET", payload.userId());
		return payload;
	}

	@Override
	public String issueMobileAuthCode(MobileAuthCodePayload payload) {
		return mobileCodeStore.issue(payload);
	}

	@Override
	public MobileAuthCodePayload consumeMobileAuthCode(String code) {
		MobileAuthCodePayload payload = mobileCodeStore.consume(code);
		if (payload == null) {
			throw new BusinessException(ErrorCode.E401_INVALID_TOKEN, "모바일 인증 코드가 유효하지 않거나 만료되었습니다.");
		}
		authSecurityEventLogger.logOneTimeCodeConsumed("MOBILE_AUTH_CODE", payload.userId());
		return payload;
	}
}



