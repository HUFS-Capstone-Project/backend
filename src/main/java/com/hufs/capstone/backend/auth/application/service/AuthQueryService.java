package com.hufs.capstone.backend.auth.application.service;

import com.hufs.capstone.backend.user.application.dto.UserProfileResult;
import com.hufs.capstone.backend.user.domain.entity.User;

public interface AuthQueryService {

	UserProfileResult getUserProfile(Long userId);

	User getUser(Long userId);
}


