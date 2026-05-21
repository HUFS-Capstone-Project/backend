package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.application.dto.LinkAnalysisResult;
import com.hufs.capstone.backend.link.application.dto.LinkPlaceResult;
import com.hufs.capstone.backend.link.application.dto.LinkStatsResult;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkCandidate;
import com.hufs.capstone.backend.link.domain.entity.RoomLinkCandidateOverride;
import com.hufs.capstone.backend.link.domain.vo.PlaceCandidateSnapshot;
import com.hufs.capstone.backend.place.application.PlaceTaxonomyReadService;
import com.hufs.capstone.backend.place.application.dto.ResolvedPlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LinkAnalysisResultAssembler {

	private final LinkPlaceCandidateSnapshotMapper placeCandidateSnapshotMapper;
	private final PlaceTaxonomyReadService placeTaxonomyReadService;

	public LinkAnalysisResultAssembler(
			LinkPlaceCandidateSnapshotMapper placeCandidateSnapshotMapper,
			PlaceTaxonomyReadService placeTaxonomyReadService
	) {
		this.placeCandidateSnapshotMapper = placeCandidateSnapshotMapper;
		this.placeTaxonomyReadService = placeTaxonomyReadService;
	}

	public LinkAnalysisResult from(Link link) {
		List<LinkPlaceResult> candidatePlaces = placeCandidateSnapshotMapper.read(link.getExtractedPlacesJson())
				.stream()
				.map(candidate -> LinkPlaceResult.fromCandidate(candidate, resolveCategory(candidate)))
				.toList();
		return new LinkAnalysisResult(
				link.getId(),
				link.getStatus(),
				link.getOriginalUrl(),
				link.getContentText(),
				new LinkStatsResult(link.getLikeCount(), link.getCommentCount(), link.getPostedAt()),
				link.getExtractionStoreName(),
				link.getExtractionAddress(),
				link.getExtractionCertainty(),
				candidatePlaces,
				link.getErrorCode(),
				link.getErrorMessage()
		);
	}

	public LinkAnalysisResult withRoomCandidateContext(
			LinkAnalysisResult result,
			List<LinkCandidate> originalCandidates,
			List<RoomLinkCandidateOverride> overrides,
			List<RoomPlace> savedPlaces
	) {
		Map<Long, RoomLinkCandidateOverride> overrideByCandidateId = overrides.stream()
				.collect(Collectors.toMap(
						override -> override.getLinkCandidate().getId(),
						Function.identity(),
						(first, ignored) -> first
				));
		List<LinkPlaceResult> overlaidCandidates = originalCandidates.stream()
				.map(candidate -> {
					RoomLinkCandidateOverride override = overrideByCandidateId.get(candidate.getId());
					return override == null
							? LinkPlaceResult.fromCandidate(candidate, resolveCategory(candidate.toSnapshot()))
							: LinkPlaceResult.fromOverride(candidate, override, resolveCategory(override.toSnapshot()));
				})
				.toList();
		LinkAnalysisResult overlaidResult = new LinkAnalysisResult(
				result.linkId(),
				result.status(),
				result.originalUrl(),
				result.contentText(),
				result.linkStats(),
				result.extractionStoreName(),
				result.extractionAddress(),
				result.extractionCertainty(),
				overlaidCandidates,
				result.errorCode(),
				result.errorMessage()
		);
		return withSavedStatus(overlaidResult, savedPlaces);
	}

	public LinkAnalysisResult withSavedStatus(LinkAnalysisResult result, List<RoomPlace> savedPlaces) {
		Map<String, RoomPlace> savedByKakaoPlaceId = savedPlaces.stream()
				.collect(Collectors.toMap(RoomPlace::getKakaoPlaceId, Function.identity(), (first, ignored) -> first));
		List<LinkPlaceResult> enrichedCandidates = result.candidatePlaces().stream()
				.map(candidate -> withSavedStatus(candidate, savedByKakaoPlaceId))
				.toList();
		return new LinkAnalysisResult(
				result.linkId(),
				result.status(),
				result.originalUrl(),
				result.contentText(),
				result.linkStats(),
				result.extractionStoreName(),
				result.extractionAddress(),
				result.extractionCertainty(),
				enrichedCandidates,
				result.errorCode(),
				result.errorMessage()
		);
	}

	private static LinkPlaceResult withSavedStatus(LinkPlaceResult candidate, Map<String, RoomPlace> savedByKakaoPlaceId) {
		if (candidate.kakaoPlaceId() == null || candidate.kakaoPlaceId().isBlank()) {
			return candidate;
		}
		RoomPlace savedPlace = savedByKakaoPlaceId.get(candidate.kakaoPlaceId());
		if (savedPlace == null) {
			return candidate;
		}
		return LinkPlaceResult.alreadyInRoom(candidate, savedPlace);
	}

	private ResolvedPlaceCategory resolveCategory(PlaceCandidateSnapshot candidate) {
		return placeTaxonomyReadService.resolveCategory(candidate.categoryGroupCode(), candidate.categoryName());
	}

	private ResolvedPlaceCategory resolveCategory(PlaceSnapshot snapshot) {
		return placeTaxonomyReadService.resolveCategory(snapshot.categoryGroupCode(), snapshot.categoryName());
	}

}
