package com.hufs.capstone.backend.place.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.place.application.dto.BusinessHoursResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlacePageResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceResult;
import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.repository.PlaceBusinessHoursRepository;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.place.domain.vo.PlaceSearchText;
import com.hufs.capstone.backend.region.application.RegionQueryService;
import com.hufs.capstone.backend.region.application.dto.RegionFilter;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import lombok.RequiredArgsConstructor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
	private final RegionQueryService regionQueryService;
	private final PlaceBusinessHoursRepository placeBusinessHoursRepository;
	private final ObjectMapper objectMapper;

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
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		int normalizedPage = page == null ? DEFAULT_PAGE : page;
		int normalizedLimit = resolveLimit(limit, size);
		if (normalizedPage < 0) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "page must be greater than or equal to 0.");
		}
		if (normalizedLimit < 1 || normalizedLimit > MAX_LIMIT) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "limit must be between 1 and 100.");
		}
		RegionFilter regionFilter = regionQueryService.validateFilter(sidoCode, sigunguCode);
		String normalizedKeyword = PlaceSearchText.normalizeKeyword(keyword);
		Page<RoomPlace> result = roomPlaceRepository.searchRoomPlaces(
				room.getId(),
				normalizedKeyword,
				PlaceSearchText.initialKeyword(keyword),
				trimToNull(categoryCode),
				normalizeTagCode(tagCode),
				regionFilter.sidoCode(),
				regionFilter.sigunguCode(),
				PageRequest.of(normalizedPage, normalizedLimit, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
		);
		Map<String, PlaceBusinessHours> cachesByKakaoPlaceId = findCaches(result.getContent());
		return new RoomPlacePageResult(
				result.getContent().stream()
						.map(roomPlace -> RoomPlaceResult.from(
								roomPlace,
								toBusinessHoursResult(cachesByKakaoPlaceId.get(roomPlace.getKakaoPlaceId()))
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
		return RoomPlaceResult.from(roomPlace, toBusinessHoursResult(cache), sourceUrl(roomPlace));
	}

	private String sourceUrl(RoomPlace roomPlace) {
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

	private BusinessHoursResult toBusinessHoursResult(PlaceBusinessHours cache) {
		if (cache == null) {
			return null;
		}
		return new BusinessHoursResult(
				readBusinessHours(cache.getBusinessHoursJson()),
				cache.getBusinessHoursStatus(),
				cache.getBusinessHoursFetchedAt(),
				cache.getBusinessHoursExpiresAt()
		);
	}

	private JsonNode readBusinessHours(String businessHoursJson) {
		if (businessHoursJson == null || businessHoursJson.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readTree(businessHoursJson);
		} catch (JsonProcessingException ex) {
			throw new BusinessException(ErrorCode.E500_INTERNAL, "Business hours cache JSON is malformed.", ex);
		}
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
