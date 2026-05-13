package com.hufs.capstone.backend.room.application;

import com.hufs.capstone.backend.room.application.port.RoomLinkCountPort;
import com.hufs.capstone.backend.room.application.port.RoomPlaceCountPort;
import com.hufs.capstone.backend.room.application.dto.RoomDetailResult;
import com.hufs.capstone.backend.room.application.dto.RoomSummaryResult;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import com.hufs.capstone.backend.room.domain.repository.RoomMemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RoomQueryService {

	private final RoomAccessService roomAccessService;
	private final RoomMemberRepository roomMemberRepository;
	private final RoomLinkCountPort roomLinkCountPort;
	private final RoomPlaceCountPort roomPlaceCountPort;

	@Transactional(readOnly = true)
	public List<RoomSummaryResult> getMyRooms(Long userId) {
		return getMyRooms(userId, null);
	}

	@Transactional(readOnly = true)
	public List<RoomSummaryResult> getMyRooms(Long userId, String keyword) {
		String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
		return roomMemberRepository.findMyRooms(userId, normalizedKeyword).stream()
				.map(this::toRoomSummary)
				.toList();
	}

	@Transactional(readOnly = true)
	public RoomDetailResult getRoomDetail(Long userId, String roomPublicId) {
		Room room = roomAccessService.getRoomOrThrow(roomPublicId);
		RoomMember membership = roomAccessService.getMembershipOrThrow(room, userId);
		long memberCount = roomMemberRepository.countByRoomId(room.getId());
		long linkCount = roomLinkCountPort.countLinksInRoom(room.getId());
		long placeCount = roomPlaceCountPort.countPlacesInRoom(room.getId());
		return new RoomDetailResult(
				room.getPublicId(),
				room.getName(),
				room.getInviteCode(),
				room.getAvatarSeed(),
				membership.isPinned(),
				memberCount,
				linkCount,
				placeCount,
				room.getCreatedAt()
		);
	}

	private RoomSummaryResult toRoomSummary(RoomMember membership) {
		Room room = membership.getRoom();
		long memberCount = roomMemberRepository.countByRoomId(room.getId());
		long linkCount = roomLinkCountPort.countLinksInRoom(room.getId());
		long placeCount = roomPlaceCountPort.countPlacesInRoom(room.getId());
		return new RoomSummaryResult(
				room.getPublicId(),
				room.getName(),
				room.getInviteCode(),
				room.getAvatarSeed(),
				membership.isPinned(),
				room.getCreatedAt(),
				memberCount,
				linkCount,
				placeCount
		);
	}
}
