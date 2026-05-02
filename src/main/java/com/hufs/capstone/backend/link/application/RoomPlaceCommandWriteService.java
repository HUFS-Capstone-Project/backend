package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.RoomPlaceSaveResult;
import com.hufs.capstone.backend.link.application.dto.RoomPlaceSaveResult.SavedPlaceResult;
import com.hufs.capstone.backend.link.application.dto.SaveRoomPlacesCommand;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.link.domain.entity.RoomPlace;
import com.hufs.capstone.backend.link.domain.repository.RoomPlaceRepository;
import com.hufs.capstone.backend.link.domain.vo.PlaceCandidateSnapshot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomPlaceCommandWriteService {

	private final LinkAnalysisAuthorizationService linkAnalysisAuthorizationService;
	private final LinkPlaceCandidateSnapshotMapper placeCandidateSnapshotMapper;
	private final RoomPlaceRepository roomPlaceRepository;

	@Transactional
	public RoomPlaceSaveResult saveRoomPlacesWithinTransaction(
			Long userId,
			String roomId,
			Long linkId,
			SaveRoomPlacesCommand command
	) {
		List<String> requestedKakaoPlaceIds = normalizeAndValidate(command.kakaoPlaceIds());
		RoomLink roomLink = linkAnalysisAuthorizationService.requireRoomLink(userId, roomId, linkId);
		Link link = roomLink.getLink();
		if (link.getStatus() != LinkAnalysisStatus.SUCCEEDED) {
			throw new BusinessException(ErrorCode.E409_CONFLICT, "Link analysis is not completed.");
		}

		Map<String, PlaceCandidateSnapshot> candidatesByKakaoPlaceId = candidateMap(link);
		validateCandidatesExist(requestedKakaoPlaceIds, candidatesByKakaoPlaceId.keySet());

		List<RoomPlace> existingPlaces = roomPlaceRepository.findByRoomIdAndKakaoPlaceIdIn(
				roomLink.getRoom().getId(),
				requestedKakaoPlaceIds
		);
		Map<String, RoomPlace> existingByKakaoPlaceId = existingPlaces.stream()
				.collect(Collectors.toMap(RoomPlace::getKakaoPlaceId, Function.identity(), (first, ignored) -> first));

		List<RoomPlace> placesToCreate = requestedKakaoPlaceIds.stream()
				.filter(kakaoPlaceId -> !existingByKakaoPlaceId.containsKey(kakaoPlaceId))
				.map(kakaoPlaceId -> RoomPlace.create(roomLink, userId, candidatesByKakaoPlaceId.get(kakaoPlaceId)))
				.toList();
		Map<String, RoomPlace> createdByKakaoPlaceId = saveNewPlaces(placesToCreate).stream()
				.collect(Collectors.toMap(RoomPlace::getKakaoPlaceId, Function.identity(), (first, ignored) -> first));

		List<SavedPlaceResult> results = new ArrayList<>();
		for (String kakaoPlaceId : requestedKakaoPlaceIds) {
			RoomPlace existing = existingByKakaoPlaceId.get(kakaoPlaceId);
			if (existing != null) {
				results.add(toResult(existing, false, true));
				continue;
			}
			results.add(toResult(createdByKakaoPlaceId.get(kakaoPlaceId), true, false));
		}
		return new RoomPlaceSaveResult(linkId, results);
	}

	private List<RoomPlace> saveNewPlaces(List<RoomPlace> placesToCreate) {
		if (placesToCreate.isEmpty()) {
			return List.of();
		}
		try {
			List<RoomPlace> saved = roomPlaceRepository.saveAll(placesToCreate);
			roomPlaceRepository.flush();
			return saved;
		} catch (DataIntegrityViolationException ex) {
			throw new RoomPlaceDuplicateRaceException(ex);
		}
	}

	private Map<String, PlaceCandidateSnapshot> candidateMap(Link link) {
		List<PlaceCandidateSnapshot> candidates;
		try {
			candidates = placeCandidateSnapshotMapper.read(link.getExtractedPlacesJson());
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.E500_INTERNAL, "Link place candidate snapshot is malformed.", ex);
		}
		Map<String, PlaceCandidateSnapshot> result = new LinkedHashMap<>();
		for (PlaceCandidateSnapshot candidate : candidates) {
			if (candidate.kakaoPlaceId() != null && !result.containsKey(candidate.kakaoPlaceId())) {
				result.put(candidate.kakaoPlaceId(), candidate);
			}
		}
		return result;
	}

	private static void validateCandidatesExist(Collection<String> requested, Set<String> candidateKakaoPlaceIds) {
		List<String> invalidIds = requested.stream()
				.filter(kakaoPlaceId -> !candidateKakaoPlaceIds.contains(kakaoPlaceId))
				.toList();
		if (!invalidIds.isEmpty()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "Requested place is not in link candidates.");
		}
	}

	private static List<String> normalizeAndValidate(List<String> kakaoPlaceIds) {
		if (kakaoPlaceIds == null || kakaoPlaceIds.isEmpty()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "kakaoPlaceIds is required.");
		}
		List<String> normalized = kakaoPlaceIds.stream()
				.map(kakaoPlaceId -> kakaoPlaceId == null ? "" : kakaoPlaceId.trim())
				.toList();
		if (normalized.stream().anyMatch(String::isBlank)) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "kakaoPlaceIds must not contain blank values.");
		}
		LinkedHashSet<String> distinct = new LinkedHashSet<>(normalized);
		if (distinct.size() != normalized.size()) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "kakaoPlaceIds must not contain duplicates.");
		}
		return List.copyOf(distinct);
	}

	private static SavedPlaceResult toResult(RoomPlace roomPlace, boolean created, boolean alreadySaved) {
		return new SavedPlaceResult(
				roomPlace.getId(),
				roomPlace.getKakaoPlaceId(),
				roomPlace.getPlaceName(),
				created,
				alreadySaved
		);
	}

	public static final class RoomPlaceDuplicateRaceException extends RuntimeException {

		RoomPlaceDuplicateRaceException(Throwable cause) {
			super("Room place duplicate race detected.", cause);
		}
	}
}
