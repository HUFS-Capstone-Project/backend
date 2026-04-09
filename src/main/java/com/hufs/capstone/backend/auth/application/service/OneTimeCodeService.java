package com.hufs.capstone.backend.auth.application.service;

import com.hufs.capstone.backend.auth.application.dto.MobileAuthCodePayload;
import com.hufs.capstone.backend.auth.application.dto.WebLoginTicketPayload;

public interface OneTimeCodeService {

	String issueWebLoginTicket(WebLoginTicketPayload payload);

	WebLoginTicketPayload consumeWebLoginTicket(String ticket);

	String issueMobileAuthCode(MobileAuthCodePayload payload);

	MobileAuthCodePayload consumeMobileAuthCode(String code);
}


