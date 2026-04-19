package com.hufs.capstone.backend.auth.application.service.impl;

import com.hufs.capstone.backend.auth.application.service.AuthQueryService;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.user.application.dto.UserProfileResult;
import com.hufs.capstone.backend.user.domain.entity.User;
import com.hufs.capstone.backend.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthQueryServiceImpl implements AuthQueryService {

	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public UserProfileResult getUserProfile(Long userId) {
		User user = userRepository.findByIdAndDeletedAtIsNull(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "사용자를 찾을 수 없습니다."));
		return UserProfileResult.from(user);
	}

	@Override
	@Transactional(readOnly = true)
	public User getUser(Long userId) {
		return userRepository.findByIdAndDeletedAtIsNull(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "사용자를 찾을 수 없습니다."));
	}
}

