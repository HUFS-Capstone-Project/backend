package com.hufs.capstone.backend.auth.application.service;

import com.hufs.capstone.backend.auth.domain.enums.RevokeReason;
import com.hufs.capstone.backend.auth.domain.vo.ClientContext;
import com.hufs.capstone.backend.auth.application.dto.TokenPair;
import com.hufs.capstone.backend.user.domain.entity.User;

public interface TokenLifecycleService {

	TokenPair issueInitial(User user, ClientContext context);

	TokenPair rotate(String presentedRefreshToken, ClientContext context);

	void revokeByRawToken(String presentedRefreshToken, RevokeReason reason);

	void revokeAllByUserId(Long userId, RevokeReason reason);
}


