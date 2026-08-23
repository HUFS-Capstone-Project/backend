package com.hufs.capstone.backend.room.application.port;

import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import java.util.List;

public interface RoomMemberSearchPort {

	List<RoomMember> findMyRooms(Long userId, String keyword);
}
