package com.hufs.capstone.backend.room.application;

import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.room.application.dto.RoomDetailResult;
import com.hufs.capstone.backend.room.application.dto.RoomSummaryResult;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import com.hufs.capstone.backend.room.domain.repository.RoomMemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomQueryService {

	private final RoomAccessService roomAccessService;
	private final RoomMemberRepository roomMemberRepository;
	private final RoomLinkRepository roomLinkRepository;

	@Transactional(readOnly = true)
	public List<RoomSummaryResult> getMyRooms(Long userId) {
		return roomMemberRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
				.map(this::toRoomSummary)
				.toList();
	}

	@Transactional(readOnly = true)
	public RoomDetailResult getRoomDetail(Long userId, String roomPublicId) {
		Room room = roomAccessService.getRoomOrThrow(roomPublicId);
		RoomMember membership = roomAccessService.getMembershipOrThrow(room, userId);
		long memberCount = roomMemberRepository.countByRoomId(room.getId());
		long linkCount = roomLinkRepository.countByRoomId(room.getId());
		return new RoomDetailResult(
				room.getPublicId(),
				room.getName(),
				room.getInviteCode(),
				membership.getRole(),
				memberCount,
				linkCount,
				room.getCreatedAt()
		);
	}

	private RoomSummaryResult toRoomSummary(RoomMember membership) {
		Room room = membership.getRoom();
		long linkCount = roomLinkRepository.countByRoomId(room.getId());
		return new RoomSummaryResult(
				room.getPublicId(),
				room.getName(),
				membership.getRole(),
				room.getCreatedAt(),
				linkCount
		);
	}
}
