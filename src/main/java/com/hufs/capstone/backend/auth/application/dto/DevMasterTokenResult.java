package com.hufs.capstone.backend.auth.application.dto;

import com.hufs.capstone.backend.user.application.dto.UserProfileResult;

public record DevMasterTokenResult(
		UserProfileResult profile,
		TokenPair tokenPair,
		boolean createdUser
) {
}
