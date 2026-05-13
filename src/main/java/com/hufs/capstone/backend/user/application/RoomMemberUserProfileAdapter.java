package com.hufs.capstone.backend.user.application;

import com.hufs.capstone.backend.room.application.port.RoomMemberUserProfilePort;
import com.hufs.capstone.backend.user.domain.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomMemberUserProfileAdapter implements RoomMemberUserProfilePort {

	private final UserRepository userRepository;

	@Override
	public List<RoomMemberUserProfile> findActiveProfiles(Collection<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return List.of();
		}
		return userRepository.findByIdInAndDeletedAtIsNull(userIds).stream()
				.map(user -> new RoomMemberUserProfile(
						user.getId(),
						user.getNickname(),
						user.getProfileImageUrl()
				))
				.toList();
	}
}
