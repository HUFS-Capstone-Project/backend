package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.global.exception.FieldValidationException;
import com.hufs.capstone.backend.link.application.dto.SaveManualRoomPlaceCommand;
import com.hufs.capstone.backend.link.application.dto.SaveRoomPlacesCommand;
import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkAnalysisRequest;
import com.hufs.capstone.backend.link.domain.entity.LinkCandidate;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.link.domain.entity.RoomLinkCandidateOverride;
import com.hufs.capstone.backend.link.domain.repository.LinkCandidateRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkCandidateOverrideRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.link.domain.vo.PlaceCandidateSnapshot;
import com.hufs.capstone.backend.place.application.RoomPlaceStorageService;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceSaveResult;
import com.hufs.capstone.backend.place.domain.enums.RoomPlaceAddedVia;
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
	private final LinkCandidateRepository linkCandidateRepository;
	private final RoomLinkCandidateOverrideRepository overrideRepository;
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
			throw new BusinessException(ErrorCode.E409_CONFLICT, "링크 분석이 완료되지 않았습니다.");
		}

		Map<String, PlaceSnapshot> candidatesByKakaoPlaceId =
				effectiveCandidateMap(link, analysisRequest.getRoom());
		validateCandidatesExist(requestedKakaoPlaceIds, candidatesByKakaoPlaceId.keySet());

		List<PlaceSnapshot> snapshots = requestedKakaoPlaceIds.stream()
				.map(candidatesByKakaoPlaceId::get)
				.toList();
		RoomLink roomLink = findOrCreateRoomLink(analysisRequest.getRoom(), link);
		return new RoomPlaceSaveResult(
				analysisRequest.getId(),
				link.getId(),
				roomPlaceStorageService.saveAll(
						analysisRequest.getRoom(),
						userId,
						snapshots,
						RoomPlaceAddedVia.LINK_ANALYSIS,
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
			throw new BusinessException(ErrorCode.E400_ILLEGAL_ARGUMENT, "장소 스냅샷은 필수입니다.");
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
						RoomPlaceAddedVia.LINK_ANALYSIS_MANUAL_SEARCH,
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
			throw new BusinessException(ErrorCode.E500_INTERNAL, "방 링크 저장에 실패했습니다.", ex);
		}
	}

	private Map<String, PlaceSnapshot> effectiveCandidateMap(Link link, Room room) {
		List<LinkCandidate> candidates = linkCandidateRepository.findByLinkIdOrderByCandidateOrderAscIdAsc(link.getId());
		if (candidates.isEmpty()) {
			return Map.of();
		}
		RoomLink roomLink = roomLinkRepository.findByRoomAndLinkId(room, link.getId()).orElse(null);
		Map<Long, RoomLinkCandidateOverride> overridesByCandidateId = roomLink == null
				? Map.of()
				: overrideRepository.findByRoomLinkId(roomLink.getId()).stream()
						.collect(
								LinkedHashMap::new,
								(map, override) -> map.putIfAbsent(override.getLinkCandidate().getId(), override),
								Map::putAll
						);
		Map<String, PlaceSnapshot> result = new LinkedHashMap<>();
		for (LinkCandidate candidate : candidates) {
			RoomLinkCandidateOverride override = overridesByCandidateId.get(candidate.getId());
			PlaceSnapshot snapshot = override == null ? toPlaceSnapshot(candidate.toSnapshot()) : override.toSnapshot();
			if (snapshot.hasKakaoPlaceId() && !result.containsKey(snapshot.kakaoPlaceId())) {
				result.put(snapshot.kakaoPlaceId(), snapshot);
			}
		}
		return result;
	}

	private static void validateCandidatesExist(Collection<String> requested, Set<String> candidateKakaoPlaceIds) {
		List<String> invalidIds = requested.stream()
				.filter(kakaoPlaceId -> !candidateKakaoPlaceIds.contains(kakaoPlaceId))
				.toList();
		if (!invalidIds.isEmpty()) {
			throw new FieldValidationException("kakaoPlaceIds", "요청한 장소가 링크 후보에 포함되어 있지 않습니다.", invalidIds);
		}
	}

	private static List<String> normalizeAndValidate(List<String> kakaoPlaceIds) {
		if (kakaoPlaceIds == null || kakaoPlaceIds.isEmpty()) {
			throw new FieldValidationException("kakaoPlaceIds", "저장할 장소는 필수입니다.");
		}
		List<String> normalized = kakaoPlaceIds.stream()
				.map(kakaoPlaceId -> kakaoPlaceId == null ? "" : kakaoPlaceId.trim())
				.toList();
		if (normalized.stream().anyMatch(String::isBlank)) {
			throw new FieldValidationException("kakaoPlaceIds", "카카오 장소 ID는 필수입니다.");
		}
		LinkedHashSet<String> distinct = new LinkedHashSet<>(normalized);
		if (distinct.size() != normalized.size()) {
			throw new FieldValidationException("kakaoPlaceIds", "중복된 카카오 장소 ID를 포함할 수 없습니다.");
		}
		return List.copyOf(distinct);
	}

	private static PlaceSnapshot toPlaceSnapshot(PlaceCandidateSnapshot candidate) {
		return PlaceSnapshot.kakao(
				candidate.kakaoPlaceId(),
				candidate.placeName(),
				candidate.categoryName(),
				candidate.categoryGroupCode(),
				candidate.phone(),
				candidate.addressName(),
				candidate.roadAddressName(),
				candidate.longitude(),
				candidate.latitude(),
				candidate.placeUrl()
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
