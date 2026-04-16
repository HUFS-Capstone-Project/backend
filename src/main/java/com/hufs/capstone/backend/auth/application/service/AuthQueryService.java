package com.hufs.capstone.backend.auth.application.service;

import com.hufs.capstone.backend.user.api.response.UserProfileResponse;
import com.hufs.capstone.backend.user.domain.entity.User;

public interface AuthQueryService {

	UserProfileResponse getUserProfile(Long userId);

	User getUser(Long userId);
}


