package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkAuthorizationService {

	private final RoomLinkRepository roomLinkRepository;

	public void assertReadable(Long userId, Long linkId) {
		boolean accessible = roomLinkRepository.existsAccessibleLinkForUser(linkId, userId);
		if (!accessible) {
			throw new BusinessException(ErrorCode.E403_FORBIDDEN, "링크 접근 권한이 없습니다.");
		}
	}
}

