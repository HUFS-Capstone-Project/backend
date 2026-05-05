package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.application.dto.SaveManualRoomPlaceCommand;
import com.hufs.capstone.backend.link.application.dto.SaveRoomPlacesCommand;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkAnalysisRequest;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.link.domain.vo.PlaceCandidateSnapshot;
import com.hufs.capstone.backend.place.application.RoomPlaceStorageService;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceSaveResult;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceSourceType;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomPlaceCommandWriteService {

	private final LinkAnalysisAuthorizationService linkAnalysisAuthorizationService;
	private final RoomLinkRepository roomLinkRepository;
	private final LinkPlaceCandidateSnapshotMapper placeCandidateSnapshotMapper;
	private final RoomPlaceStorageService roomPlaceStorageService;

	@Transactional
	public RoomPlaceSaveResult saveRoomPlacesWithinTransaction(
			Long userId,
			String roomId,
			Long analysisRequestId,
			SaveRoomPlacesCommand command
	) {
		List<String> requestedKakaoPlaceIds = normalizeAndValidate(command.kakaoPlaceIds());
		LinkAnalysisRequest analysisRequest =
				linkAnalysisAuthorizationService.requireAnalysisRequestForUpdate(userId, roomId, analysisRequestId);
		Link link = analysisRequest.getLink();
		if (link.getStatus() != LinkAnalysisStatus.SUCCEEDED) {
			throw new BusinessException(ErrorCode.E409_CONFLICT, "Link analysis is not completed.");
		}

		Map<String, PlaceCandidateSnapshot> candidatesByKakaoPlaceId = candidateMap(link);
		validateCandidatesExist(requestedKakaoPlaceIds, candidatesByKakaoPlaceId.keySet());

		List<PlaceSnapshot> snapshots = requestedKakaoPlaceIds.stream()
				.map(kakaoPlaceId -> toPlaceSnapshot(candidatesByKakaoPlaceId.get(kakaoPlaceId)))
				.toList();
		RoomLink roomLink = findOrCreateRoomLink(analysisRequest.getRoom(), link);
		return new RoomPlaceSaveResult(
				analysisRequest.getId(),
				link.getId(),
				roomPlaceStorageService.saveAll(
						analysisRequest.getRoom(),
						userId,
						snapshots,
						null,
						RoomPlaceSourceType.LINK_ANALYSIS,
						roomLink
				)
		);
	}

	@Transactional
	public RoomPlaceSaveResult saveManualRoomPlaceWithinTransaction(
			Long userId,
			String roomId,
			Long analysisRequestId,
			SaveManualRoomPlaceCommand command
	) {
		if (command == null || command.snapshot() == null) {
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "Place snapshot is required.");
		}
		LinkAnalysisRequest analysisRequest =
				linkAnalysisAuthorizationService.requireAnalysisRequestForUpdate(userId, roomId, analysisRequestId);
		Link link = analysisRequest.getLink();
		RoomLink roomLink = findOrCreateRoomLink(analysisRequest.getRoom(), link);
		return new RoomPlaceSaveResult(
				analysisRequest.getId(),
				link.getId(),
				roomPlaceStorageService.saveAll(
						analysisRequest.getRoom(),
						userId,
						List.of(command.snapshot()),
						null,
						RoomPlaceSourceType.LINK_ANALYSIS_MANUAL_SEARCH,
						roomLink
				)
		);
	}

	private RoomLink findOrCreateRoomLink(Room room, Link link) {
		RoomLink existing = roomLinkRepository.findByRoomAndLinkId(room, link.getId()).orElse(null);
		if (existing != null) {
			return existing;
		}
		try {
			return roomLinkRepository.saveAndFlush(RoomLink.bind(room, link));
		} catch (DataIntegrityViolationException ex) {
			throw new RoomLinkDuplicateRaceException(room.getPublicId(), link.getId(), ex);
		} catch (DataAccessException ex) {
			log.error("Room link save failed. roomId={}, linkId={}", room.getPublicId(), link.getId(), ex);
			throw new BusinessException(ErrorCode.E500_INTERNAL, "Room link save failed.", ex);
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
			if (candidate.kakaoPlaceId() != null && !candidate.kakaoPlaceId().isBlank()
					&& !result.containsKey(candidate.kakaoPlaceId())) {
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

	private static PlaceSnapshot toPlaceSnapshot(PlaceCandidateSnapshot candidate) {
		return PlaceSnapshot.kakao(
				candidate.kakaoPlaceId(),
				candidate.placeName(),
				candidate.categoryName(),
				candidate.categoryGroupCode(),
				candidate.categoryGroupName(),
				candidate.phone(),
				candidate.addressName(),
				candidate.roadAddressName(),
				candidate.longitude(),
				candidate.latitude(),
				candidate.placeUrl(),
				candidate.confidence(),
				candidate.sourceKeyword(),
				candidate.sourceSentence(),
				candidate.rawCandidate()
		);
	}

	public static final class RoomLinkDuplicateRaceException extends RuntimeException {

		private final String roomId;
		private final Long linkId;

		RoomLinkDuplicateRaceException(String roomId, Long linkId, Throwable cause) {
			super("Room link duplicate race detected: roomId=" + roomId + ", linkId=" + linkId, cause);
			this.roomId = roomId;
			this.linkId = linkId;
		}

		public String roomId() {
			return roomId;
		}

		public Long linkId() {
			return linkId;
		}
	}
}
