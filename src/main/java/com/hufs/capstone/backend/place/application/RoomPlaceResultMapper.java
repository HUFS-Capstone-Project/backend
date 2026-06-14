package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.place.application.dto.BusinessHoursResult;
import com.hufs.capstone.backend.place.application.dto.MyRoomPlaceResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceMemoResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceResult;
import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.entity.RoomPlaceMemo;
import com.hufs.capstone.backend.place.domain.repository.PlaceBusinessHoursRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceMemoRepository;
import com.hufs.capstone.backend.room.application.port.RoomMemberUserProfilePort;
import com.hufs.capstone.backend.room.application.port.RoomMemberUserProfilePort.RoomMemberUserProfile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RoomPlaceResultMapper {

	private final RoomPlaceMemoRepository roomPlaceMemoRepository;
	private final RoomMemberUserProfilePort roomMemberUserProfilePort;
	private final PlaceBusinessHoursRepository placeBusinessHoursRepository;
	private final BusinessHoursDisplayResolver businessHoursDisplayResolver;

	List<RoomPlaceResult> toRoomPlaceResults(Collection<RoomPlace> roomPlaces, Long viewerUserId) {
		MappingContext context = loadContext(roomPlaces);
		return roomPlaces.stream()
				.map(roomPlace -> toRoomPlaceResult(roomPlace, viewerUserId, context))
				.toList();
	}

	List<MyRoomPlaceResult> toMyRoomPlaceResults(Collection<RoomPlace> roomPlaces, Long viewerUserId) {
		MappingContext context = loadContext(roomPlaces);
		return roomPlaces.stream()
				.map(roomPlace -> new MyRoomPlaceResult(
						toRoomPlaceResult(roomPlace, viewerUserId, context),
						MyRoomPlaceResult.RoomResult.from(roomPlace.getRoom())
				))
				.toList();
	}

	RoomPlaceResult toRoomPlaceResult(RoomPlace roomPlace, Long viewerUserId) {
		return toRoomPlaceResult(roomPlace, viewerUserId, loadContext(List.of(roomPlace)));
	}

	private RoomPlaceResult toRoomPlaceResult(RoomPlace roomPlace, Long viewerUserId, MappingContext context) {
		return RoomPlaceResult.from(
				roomPlace,
				toBusinessHoursResult(context.cachesByKakaoPlaceId().get(roomPlace.getKakaoPlaceId())),
				originalUrl(roomPlace),
				context.memosByRoomPlaceId().get(roomPlace.getId()),
				viewerUserId
		);
	}

	private MappingContext loadContext(Collection<RoomPlace> roomPlaces) {
		if (roomPlaces == null || roomPlaces.isEmpty()) {
			return new MappingContext(Map.of(), Map.of());
		}
		return new MappingContext(findCaches(roomPlaces), findMemoResults(roomPlaces));
	}

	private String originalUrl(RoomPlace roomPlace) {
		if (roomPlace.getOriginRoomLink() == null || roomPlace.getOriginRoomLink().getLink() == null) {
			return null;
		}
		return roomPlace.getOriginRoomLink().getLink().getOriginalUrl();
	}

	private Map<String, PlaceBusinessHours> findCaches(Collection<RoomPlace> roomPlaces) {
		List<String> kakaoPlaceIds = roomPlaces.stream()
				.map(RoomPlace::getKakaoPlaceId)
				.filter(kakaoPlaceId -> kakaoPlaceId != null && !kakaoPlaceId.isBlank())
				.toList();
		if (kakaoPlaceIds.isEmpty()) {
			return Map.of();
		}
		return placeBusinessHoursRepository.findByKakaoPlaceIdIn(kakaoPlaceIds)
				.stream()
				.collect(Collectors.toMap(
						PlaceBusinessHours::getKakaoPlaceId,
						Function.identity(),
						(first, second) -> first
				));
	}

	private Map<Long, List<RoomPlaceMemoResult>> findMemoResults(Collection<RoomPlace> roomPlaces) {
		List<Long> roomPlaceIds = roomPlaces.stream()
				.map(RoomPlace::getId)
				.toList();
		List<RoomPlaceMemo> memos = roomPlaceMemoRepository.findByRoomPlaceIdInOrderByUpdatedAtAscIdAsc(roomPlaceIds);
		Set<Long> authorUserIds = new LinkedHashSet<>();
		memos.forEach(memo -> authorUserIds.add(memo.getUserId()));
		Map<Long, RoomMemberUserProfile> profilesByUserId = findProfiles(authorUserIds);
		Map<Long, List<RoomPlaceMemoResult>> resultsByRoomPlaceId = new HashMap<>();
		memos.forEach(memo -> resultsByRoomPlaceId
				.computeIfAbsent(memo.getRoomPlaceId(), ignored -> new ArrayList<>())
				.add(toMemoResult(memo, profilesByUserId)));
		return resultsByRoomPlaceId.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> List.copyOf(entry.getValue())
				));
	}

	private Map<Long, RoomMemberUserProfile> findProfiles(Set<Long> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		return roomMemberUserProfilePort.findActiveProfiles(userIds).stream()
				.collect(Collectors.toMap(
						RoomMemberUserProfile::userId,
						Function.identity(),
						(first, second) -> first
				));
	}

	private RoomPlaceMemoResult toMemoResult(
			RoomPlaceMemo memo,
			Map<Long, RoomMemberUserProfile> profilesByUserId
	) {
		RoomMemberUserProfile profile = profilesByUserId.get(memo.getUserId());
		return new RoomPlaceMemoResult(
				memo.getUserId(),
				profile == null ? null : profile.nickname(),
				profile == null ? null : profile.profileImageUrl(),
				memo.getMemo(),
				memo.getUpdatedAt()
		);
	}

	private BusinessHoursResult toBusinessHoursResult(PlaceBusinessHours cache) {
		if (cache == null) {
			return null;
		}
		return new BusinessHoursResult(
				businessHoursDisplayResolver.resolve(cache.getBusinessHoursJson(), cache.getBusinessHoursStatus()),
				cache.getBusinessHoursStatus(),
				cache.getBusinessHoursFetchedAt(),
				cache.getBusinessHoursExpiresAt()
		);
	}

	private record MappingContext(
			Map<String, PlaceBusinessHours> cachesByKakaoPlaceId,
			Map<Long, List<RoomPlaceMemoResult>> memosByRoomPlaceId
	) {
	}
}
