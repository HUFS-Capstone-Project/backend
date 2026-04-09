package com.hufs.capstone.backend.auth.application.service;

import com.hufs.capstone.backend.auth.api.response.MeResponse;
import com.hufs.capstone.backend.user.domain.entity.User;

public interface AuthQueryService {

	MeResponse getMe(Long userId);

	User getUser(Long userId);
}


