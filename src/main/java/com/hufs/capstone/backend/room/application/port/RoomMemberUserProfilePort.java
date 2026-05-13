package com.hufs.capstone.backend.room.application.port;

import java.util.Collection;
import java.util.List;

public interface RoomMemberUserProfilePort {

	List<RoomMemberUserProfile> findActiveProfiles(Collection<Long> userIds);

	record RoomMemberUserProfile(
			Long userId,
			String nickname,
			String profileImageUrl
	) {
	}
}
