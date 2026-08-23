package com.hufs.capstone.backend.room.application;

import com.hufs.capstone.backend.room.application.port.RoomLinkCountPort;
import com.hufs.capstone.backend.room.application.port.RoomMemberUserProfilePort;
import com.hufs.capstone.backend.room.application.port.RoomMemberUserProfilePort.RoomMemberUserProfile;
import com.hufs.capstone.backend.room.application.port.RoomMemberSearchPort;
import com.hufs.capstone.backend.room.application.port.RoomPlaceCountPort;
import com.hufs.capstone.backend.room.application.dto.RoomDetailResult;
import com.hufs.capstone.backend.room.application.dto.RoomMemberProfileResult;
import com.hufs.capstone.backend.room.application.dto.RoomSummaryResult;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import com.hufs.capstone.backend.room.domain.repository.RoomMemberRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RoomQueryService {

	private final RoomAccessService roomAccessService;
	private final RoomMemberRepository roomMemberRepository;
	private final RoomMemberSearchPort roomMemberSearchPort;
	private final RoomLinkCountPort roomLinkCountPort;
	private final RoomPlaceCountPort roomPlaceCountPort;
	private final RoomMemberUserProfilePort roomMemberUserProfilePort;

	@Transactional(readOnly = true)
	public List<RoomSummaryResult> getMyRooms(Long userId) {
		return getMyRooms(userId, null);
	}

	@Transactional(readOnly = true)
	public List<RoomSummaryResult> getMyRooms(Long userId, String keyword) {
		String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
		List<RoomMember> memberships = roomMemberSearchPort.findMyRooms(userId, normalizedKeyword);
		List<Long> roomIds = memberships.stream()
				.map(RoomMember::getRoom)
				.map(Room::getId)
				.toList();
		Map<Long, Long> memberCounts = countMembersByRoomIds(roomIds);
		Map<Long, Long> linkCounts = roomLinkCountPort.countLinksByRoomIds(roomIds);
		Map<Long, Long> placeCounts = roomPlaceCountPort.countPlacesByRoomIds(roomIds);
		return memberships.stream()
				.map(membership -> toRoomSummary(membership, memberCounts, linkCounts, placeCounts))
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

	@Transactional(readOnly = true)
	public List<RoomMemberProfileResult> getRoomMembers(Long userId, String roomPublicId) {
		Room room = roomAccessService.requireMemberRoom(roomPublicId, userId);
		List<RoomMember> members = roomMemberRepository.findByRoomIdOrderByCreatedAtAscIdAsc(room.getId());
		List<Long> memberUserIds = members.stream()
				.map(RoomMember::getUserId)
				.toList();
		Map<Long, RoomMemberUserProfile> profilesByUserId = roomMemberUserProfilePort.findActiveProfiles(memberUserIds)
				.stream()
				.collect(Collectors.toMap(
						RoomMemberUserProfile::userId,
						Function.identity(),
						(first, second) -> first
				));
		return members.stream()
				.map(member -> {
					RoomMemberUserProfile profile = profilesByUserId.get(member.getUserId());
					return new RoomMemberProfileResult(
							member.getUserId(),
							profile == null ? null : profile.nickname(),
							profile == null ? null : profile.profileImageUrl(),
							member.getCreatedAt(),
							member.getUserId().equals(userId)
					);
				})
				.toList();
	}

	private RoomSummaryResult toRoomSummary(
			RoomMember membership,
			Map<Long, Long> memberCounts,
			Map<Long, Long> linkCounts,
			Map<Long, Long> placeCounts
	) {
		Room room = membership.getRoom();
		long memberCount = memberCounts.getOrDefault(room.getId(), 0L);
		long linkCount = linkCounts.getOrDefault(room.getId(), 0L);
		long placeCount = placeCounts.getOrDefault(room.getId(), 0L);
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

	private Map<Long, Long> countMembersByRoomIds(List<Long> roomIds) {
		if (roomIds.isEmpty()) {
			return Map.of();
		}
		return roomMemberRepository.countByRoomIds(roomIds).stream()
				.collect(Collectors.toMap(
						RoomMemberRepository.RoomCountProjection::getRoomId,
						RoomMemberRepository.RoomCountProjection::getCount
				));
	}
}
