package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.global.exception.FieldValidationException;
import com.hufs.capstone.backend.global.pagination.CursorPageResult;
import com.hufs.capstone.backend.place.application.dto.MyRoomPlacePageResult;
import com.hufs.capstone.backend.place.application.dto.MyRoomPlaceResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceCursor;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceMapItemResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceMapResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlacePageResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceResult;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.region.application.RegionQueryService;
import com.hufs.capstone.backend.region.application.dto.RegionFilter;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.math.BigDecimal;
import java.util.List;
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
	private static final int MAP_PLACE_LIMIT = 500;
	private static final String ALL_TAG_CODE = "ALL";

	private final RoomAccessService roomAccessService;
	private final RoomPlaceRepository roomPlaceRepository;
	private final RegionQueryService regionQueryService;
	private final RoomPlaceResultMapper roomPlaceResultMapper;
	private final RoomPlaceCursorPageAssembler cursorPageAssembler;

	@Transactional(readOnly = true)
	public CursorPageResult<RoomPlaceResult> searchRoomPlaces(
			Long userId,
			String roomId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Long createdBy,
			Integer limit,
			String cursor
	) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		validateCreatedByFilter(room, createdBy);
		int normalizedLimit = validateLimit(limit);
		RegionFilter regionFilter = regionQueryService.validateFilter(sidoCode, sigunguCode);
		String normalizedKeyword = trimToNull(keyword);
		String normalizedCategoryCode = trimToNull(categoryCode);
		String normalizedTagCode = normalizeTagCode(tagCode);
		long totalCount = roomPlaceRepository.countByRoomId(room.getId());
		RoomPlaceCursor decodedCursor = cursorPageAssembler.decode(cursor);
		List<RoomPlace> fetched = roomPlaceRepository.searchRoomPlacesAfterCursor(
				room.getId(),
				normalizedKeyword,
				normalizedCategoryCode,
				normalizedTagCode,
				regionFilter.sidoCode(),
				regionFilter.sigunguCode(),
				createdBy,
				decodedCursor == null ? null : decodedCursor.createdAt(),
				decodedCursor == null ? null : decodedCursor.roomPlaceId(),
				normalizedLimit + 1
		);
		boolean hasNext = fetched.size() > normalizedLimit;
		List<RoomPlace> roomPlaces = hasNext ? fetched.subList(0, normalizedLimit) : fetched;
		List<RoomPlaceResult> items = roomPlaceResultMapper.toRoomPlaceResults(roomPlaces, userId);
		return cursorPageAssembler.assemble(items, roomPlaces, normalizedLimit, totalCount, hasNext);
	}

	@Transactional(readOnly = true)
	public RoomPlaceMapResult findMapPlaces(
			Long userId,
			String roomId,
			BigDecimal swLat,
			BigDecimal swLng,
			BigDecimal neLat,
			BigDecimal neLng
	) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		Bounds bounds = validateBounds(swLat, swLng, neLat, neLng);
		List<RoomPlace> fetched = roomPlaceRepository.findMapPlacesInBounds(
				room.getId(),
				bounds.minLat(),
				bounds.maxLat(),
				bounds.minLng(),
				bounds.maxLng(),
				MAP_PLACE_LIMIT + 1
		);
		boolean truncated = fetched.size() > MAP_PLACE_LIMIT;
		List<RoomPlace> roomPlaces = truncated ? fetched.subList(0, MAP_PLACE_LIMIT) : fetched;
		return new RoomPlaceMapResult(
				roomPlaces.stream()
						.map(RoomPlaceMapItemResult::from)
						.toList(),
				MAP_PLACE_LIMIT,
				truncated
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
			throw new FieldValidationException("page", "page는 0 이상이어야 합니다.", normalizedPage);
		}
		if (normalizedLimit < 1 || normalizedLimit > MAX_LIMIT) {
			throw new FieldValidationException("limit", "limit는 1~100 사이여야 합니다.", normalizedLimit);
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
		return new RoomPlacePageResult(
				roomPlaceResultMapper.toRoomPlaceResults(roomPlaces, userId),
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
				.orElseThrow(() -> new BusinessException(ErrorCode.E404_NOT_FOUND, "방 장소를 찾을 수 없습니다."));
		return roomPlaceResultMapper.toRoomPlaceResult(roomPlace, userId);
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
			throw new FieldValidationException("page", "page는 0 이상이어야 합니다.", normalizedPage);
		}
		if (normalizedLimit < 1 || normalizedLimit > MAX_LIMIT) {
			throw new FieldValidationException("limit", "limit는 1~100 사이여야 합니다.", normalizedLimit);
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
		return new MyRoomPlacePageResult(
				roomPlaceResultMapper.toMyRoomPlaceResults(roomPlaces, userId),
				normalizedPage,
				normalizedLimit,
				result.getTotalElements(),
				result.getTotalPages()
		);
	}

	@Transactional(readOnly = true)
	public CursorPageResult<MyRoomPlaceResult> searchMyRoomPlaces(
			Long userId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Integer limit,
			String cursor
	) {
		int normalizedLimit = validateLimit(limit);
		RegionFilter regionFilter = regionQueryService.validateFilter(sidoCode, sigunguCode);
		String normalizedKeyword = trimToNull(keyword);
		String normalizedCategoryCode = trimToNull(categoryCode);
		String normalizedTagCode = normalizeTagCode(tagCode);
		long totalCount = roomPlaceRepository.countMyRoomPlaces(
				userId,
				null,
				null,
				null,
				null,
				null
		);
		RoomPlaceCursor decodedCursor = cursorPageAssembler.decode(cursor);
		List<RoomPlace> fetched = roomPlaceRepository.searchMyRoomPlacesAfterCursor(
				userId,
				normalizedKeyword,
				normalizedCategoryCode,
				normalizedTagCode,
				regionFilter.sidoCode(),
				regionFilter.sigunguCode(),
				decodedCursor == null ? null : decodedCursor.createdAt(),
				decodedCursor == null ? null : decodedCursor.roomPlaceId(),
				normalizedLimit + 1
		);
		boolean hasNext = fetched.size() > normalizedLimit;
		List<RoomPlace> roomPlaces = hasNext ? fetched.subList(0, normalizedLimit) : fetched;
		List<MyRoomPlaceResult> items = roomPlaceResultMapper.toMyRoomPlaceResults(roomPlaces, userId);
		return cursorPageAssembler.assemble(items, roomPlaces, normalizedLimit, totalCount, hasNext);
	}

	private void validateCreatedByFilter(Room room, Long createdBy) {
		if (createdBy != null) {
			roomAccessService.getMembershipOrThrow(room, createdBy);
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

	private int validateLimit(Integer limit) {
		int normalizedLimit = limit == null ? DEFAULT_LIMIT : limit;
		if (normalizedLimit < 1 || normalizedLimit > MAX_LIMIT) {
			throw new FieldValidationException("limit", "limit must be between 1 and 100.", normalizedLimit);
		}
		return normalizedLimit;
	}

	private static Bounds validateBounds(BigDecimal swLat, BigDecimal swLng, BigDecimal neLat, BigDecimal neLng) {
		if (swLat == null) {
			throw new FieldValidationException("swLat", "swLat is required.", null);
		}
		if (swLng == null) {
			throw new FieldValidationException("swLng", "swLng is required.", null);
		}
		if (neLat == null) {
			throw new FieldValidationException("neLat", "neLat is required.", null);
		}
		if (neLng == null) {
			throw new FieldValidationException("neLng", "neLng is required.", null);
		}
		BigDecimal minLat = swLat.min(neLat);
		BigDecimal maxLat = swLat.max(neLat);
		BigDecimal minLng = swLng.min(neLng);
		BigDecimal maxLng = swLng.max(neLng);
		if (minLat.compareTo(BigDecimal.valueOf(-90)) < 0 || maxLat.compareTo(BigDecimal.valueOf(90)) > 0) {
			throw new FieldValidationException("latitude", "Latitude must be between -90 and 90.", null);
		}
		if (minLng.compareTo(BigDecimal.valueOf(-180)) < 0 || maxLng.compareTo(BigDecimal.valueOf(180)) > 0) {
			throw new FieldValidationException("longitude", "Longitude must be between -180 and 180.", null);
		}
		return new Bounds(minLat, maxLat, minLng, maxLng);
	}

	private record Bounds(
			BigDecimal minLat,
			BigDecimal maxLat,
			BigDecimal minLng,
			BigDecimal maxLng
	) {
	}
}
