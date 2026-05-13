package com.hufs.capstone.backend.room.domain.repository;

import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import java.util.List;

public interface RoomMemberSearchRepository {

	List<RoomMember> findMyRooms(Long userId, String keyword);
}
