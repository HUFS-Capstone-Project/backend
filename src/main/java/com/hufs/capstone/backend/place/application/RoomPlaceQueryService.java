package com.hufs.capstone.backend.place.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.place.application.dto.RoomPlacePageResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceResult;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.place.domain.vo.PlaceSearchText;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
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

	@Transactional(readOnly = true)
	public RoomPlacePageResult searchRoomPlaces(
			Long userId,
			String roomId,
			String keyword,
			String categoryCode,
			String tagCode,
			Integer page,
			Integer limit
	) {
		Room room = roomAccessService.requireMemberRoom(roomId, userId);
		int normalizedPage = page == null ? DEFAULT_PAGE : page;
		int normalizedLimit = limit == null ? DEFAULT_LIMIT : limit;
		if (normalizedPage < 0) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "page must be greater than or equal to 0.");
		}
		if (normalizedLimit < 1 || normalizedLimit > MAX_LIMIT) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "limit must be between 1 and 100.");
		}
		String normalizedKeyword = PlaceSearchText.normalizeKeyword(keyword);
		Page<RoomPlace> result = roomPlaceRepository.searchRoomPlaces(
				room.getId(),
				normalizedKeyword,
				PlaceSearchText.initialKeyword(keyword),
				trimToNull(categoryCode),
				normalizeTagCode(tagCode),
				PageRequest.of(normalizedPage, normalizedLimit, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
		);
		return new RoomPlacePageResult(
				result.getContent().stream()
						.map(RoomPlaceResult::from)
						.toList(),
				normalizedPage,
				normalizedLimit,
				result.getTotalElements(),
				result.getTotalPages()
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
}
