package com.hufs.capstone.backend.room.application;

import com.hufs.capstone.backend.room.application.dto.RoomDetailResult;
import com.hufs.capstone.backend.room.application.dto.RoomSummaryResult;
import java.util.List;

public interface RoomQueryService {

	List<RoomSummaryResult> getMyRooms(Long userId);

	RoomDetailResult getRoomDetail(Long userId, String roomPublicId);
}
