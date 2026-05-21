package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.place.application.dto.BusinessHoursResult;
import com.hufs.capstone.backend.place.application.dto.MyRoomPlacePageResult;
import com.hufs.capstone.backend.place.application.dto.MyRoomPlaceResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlacePageResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceMemoResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceResult;
import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.entity.RoomPlaceMemo;
import com.hufs.capstone.backend.place.domain.repository.PlaceBusinessHoursRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceMemoRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.region.application.RegionQueryService;
import com.hufs.capstone.backend.region.application.dto.RegionFilter;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.application.port.RoomMemberUserProfilePort;
import com.hufs.capstone.backend.room.application.port.RoomMemberUserProfilePort.RoomMemberUserProfile;
import com.hufs.capstone.backend.room.domain.entity.Room;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomPlaceQueryService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_LIMIT = 20;
	private static final int MAX_LIMIT = 100;
	private static final String ALL_TAG_CODE = "ALL";

	private final RoomAccessService roomAccessService;
	private final RoomPlaceRepository roomPlaceRepository;
	private final RoomPlaceMemoRepository roomPlaceMemoRepository;
	private final RoomMemberUserProfilePort roomMemberUserProfilePort;
	private final RegionQueryService regionQueryService;
	private final PlaceBusinessHoursRepository placeBusinessHoursRepository;
	private final BusinessHoursDisplayResolver businessHoursDisplayResolver;

	@Transactional(readOnly = true)
	public RoomPlacePageResult searchRoomPlaces(
			Long userId,
			String roomId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Integer page,
			Integer limit,
			Integer size
	) {
		return searchRoomPlaces(
				userId,
				roomId,
				keyword,
				categoryCode,
				tagCode,
				sidoCode,
				sigunguCode,
				null,
				page,
				limit,
				size
		);
	}

	@Transactional(readOnly = true)
	public RoomPlacePageResult searchRoomPlaces(
			Long userId,
			String roomId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Long createdBy,
			Integer page,
			Integer limit,
			Integer size
	) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		validateCreatedByFilter(room, createdBy);
		int normalizedPage = page == null ? DEFAULT_PAGE : page;
		int normalizedLimit = resolveLimit(limit, size);
		if (normalizedPage < 0) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "page must be greater than or equal to 0.");
		}
		if (normalizedLimit < 1 || normalizedLimit > MAX_LIMIT) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "limit must be between 1 and 100.");
		}
		RegionFilter regionFilter = regionQueryService.validateFilter(sidoCode, sigunguCode);
		Page<RoomPlace> result = roomPlaceRepository.searchRoomPlaces(
				room.getId(),
				trimToNull(keyword),
				trimToNull(categoryCode),
				normalizeTagCode(tagCode),
				regionFilter.sidoCode(),
				regionFilter.sigunguCode(),
				createdBy,
				PageRequest.of(normalizedPage, normalizedLimit, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
		);
		List<RoomPlace> roomPlaces = result.getContent();
		Map<String, PlaceBusinessHours> cachesByKakaoPlaceId = findCaches(roomPlaces);
		Map<Long, List<RoomPlaceMemoResult>> memosByRoomPlaceId = findMemoResults(roomPlaces);
		return new RoomPlacePageResult(
				roomPlaces.stream()
						.map(roomPlace -> RoomPlaceResult.from(
								roomPlace,
								toBusinessHoursResult(cachesByKakaoPlaceId.get(roomPlace.getKakaoPlaceId())),
								null,
								memosByRoomPlaceId.get(roomPlace.getId()),
								userId
						))
						.toList(),
				normalizedPage,
				normalizedLimit,
				result.getTotalElements(),
				result.getTotalPages()
		);
	}

	@Transactional(readOnly = true)
	public RoomPlaceResult getRoomPlace(Long userId, String roomId, Long roomPlaceId) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		RoomPlace roomPlace = roomPlaceRepository.findByIdAndRoomId(roomPlaceId, room.getId())
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "Room place not found."));
		PlaceBusinessHours cache = placeBusinessHoursRepository.findByKakaoPlaceId(roomPlace.getKakaoPlaceId())
				.orElse(null);
		Map<Long, List<RoomPlaceMemoResult>> memosByRoomPlaceId = findMemoResults(List.of(roomPlace));
		return RoomPlaceResult.from(
				roomPlace,
				toBusinessHoursResult(cache),
				originalUrl(roomPlace),
				memosByRoomPlaceId.get(roomPlace.getId()),
				userId
		);
	}

	@Transactional(readOnly = true)
	public MyRoomPlacePageResult searchMyRoomPlaces(
			Long userId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Integer page,
			Integer limit,
			Integer size
	) {
		int normalizedPage = page == null ? DEFAULT_PAGE : page;
		int normalizedLimit = resolveLimit(limit, size);
		if (normalizedPage < 0) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "page must be greater than or equal to 0.");
		}
		if (normalizedLimit < 1 || normalizedLimit > MAX_LIMIT) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "limit must be between 1 and 100.");
		}
		RegionFilter regionFilter = regionQueryService.validateFilter(sidoCode, sigunguCode);
		Page<RoomPlace> result = roomPlaceRepository.searchMyRoomPlaces(
				userId,
				trimToNull(keyword),
				trimToNull(categoryCode),
				normalizeTagCode(tagCode),
				regionFilter.sidoCode(),
				regionFilter.sigunguCode(),
				PageRequest.of(normalizedPage, normalizedLimit, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
		);
		List<RoomPlace> roomPlaces = result.getContent();
		Map<String, PlaceBusinessHours> cachesByKakaoPlaceId = findCaches(roomPlaces);
		Map<Long, List<RoomPlaceMemoResult>> memosByRoomPlaceId = findMemoResults(roomPlaces);
		return new MyRoomPlacePageResult(
				roomPlaces.stream()
						.map(roomPlace -> new MyRoomPlaceResult(
								RoomPlaceResult.from(
										roomPlace,
										toBusinessHoursResult(cachesByKakaoPlaceId.get(roomPlace.getKakaoPlaceId())),
										null,
										memosByRoomPlaceId.get(roomPlace.getId()),
										userId
								),
								MyRoomPlaceResult.RoomResult.from(roomPlace.getRoom())
						))
						.toList(),
				normalizedPage,
				normalizedLimit,
				result.getTotalElements(),
				result.getTotalPages()
		);
	}

	private void validateCreatedByFilter(Room room, Long createdBy) {
		if (createdBy != null) {
			roomAccessService.getMembershipOrThrow(room, createdBy);
		}
	}

	private String originalUrl(RoomPlace roomPlace) {
		if (roomPlace.getSourceRoomLink() == null || roomPlace.getSourceRoomLink().getLink() == null) {
			return null;
		}
		return roomPlace.getSourceRoomLink().getLink().getOriginalUrl();
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
		if (roomPlaces == null || roomPlaces.isEmpty()) {
			return Map.of();
		}
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

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String normalizeTagCode(String value) {
		String normalized = trimToNull(value);
		return ALL_TAG_CODE.equalsIgnoreCase(normalized) ? null : normalized;
	}

	private static int resolveLimit(Integer limit, Integer size) {
		if (size != null) {
			return size;
		}
		if (limit != null) {
			return limit;
		}
		return DEFAULT_LIMIT;
	}
}
