package com.hufs.capstone.backend.room.application;

import com.hufs.capstone.backend.room.application.dto.CreateRoomResult;
import com.hufs.capstone.backend.room.application.dto.JoinRoomResult;

public interface RoomCommandService {

	CreateRoomResult createRoom(Long userId, String roomName);

	JoinRoomResult joinByInviteCode(Long userId, String inviteCode, String ipAddress);

	void renameRoom(Long userId, String roomId, String roomName);

	void updateRoomPin(Long userId, String roomId, boolean pinned);

	void leaveRoom(Long userId, String roomId);
}
