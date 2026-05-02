package com.hufs.capstone.backend.link.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.external.processing.dto.ProcessingJobResultResponse.PlaceCandidateResponse;
import com.hufs.capstone.backend.link.domain.vo.PlaceCandidateSnapshot;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LinkPlaceCandidateSnapshotMapper {

	private static final TypeReference<List<PlaceCandidateSnapshot>> SNAPSHOT_LIST_TYPE = new TypeReference<>() {
	};

	private final ObjectMapper objectMapper;

	public LinkPlaceCandidateSnapshotMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public List<PlaceCandidateSnapshot> fromProcessingCandidates(List<PlaceCandidateResponse> candidates) {
		if (candidates == null || candidates.isEmpty()) {
			return List.of();
		}
		return candidates.stream()
				.map(this::fromProcessingCandidate)
				.toList();
	}

	public String write(List<PlaceCandidateSnapshot> candidates) {
		try {
			return objectMapper.writeValueAsString(candidates == null ? List.of() : candidates);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Cannot serialize extracted place candidates.", ex);
		}
	}

	public List<PlaceCandidateSnapshot> read(String candidatesJson) {
		if (candidatesJson == null || candidatesJson.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(candidatesJson, SNAPSHOT_LIST_TYPE);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Cannot deserialize extracted place candidates.", ex);
		}
	}

	private PlaceCandidateSnapshot fromProcessingCandidate(PlaceCandidateResponse candidate) {
		if (candidate == null) {
			return new PlaceCandidateSnapshot(
					null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
			);
		}
		return new PlaceCandidateSnapshot(
				trimToNull(candidate.kakaoPlaceId()),
				trimToNull(candidate.placeName()),
				trimToNull(candidate.categoryName()),
				trimToNull(candidate.categoryGroupCode()),
				trimToNull(candidate.categoryGroupName()),
				trimToNull(candidate.phone()),
				trimToNull(candidate.addressName()),
				trimToNull(candidate.roadAddressName()),
				parseDecimal(candidate.x(), "selected_places.x"),
				parseDecimal(candidate.y(), "selected_places.y"),
				trimToNull(candidate.placeUrl()),
				candidate.confidence(),
				trimToNull(candidate.sourceKeyword()),
				trimToNull(candidate.sourceSentence()),
				trimToNull(candidate.rawCandidate())
		);
	}

	private static BigDecimal parseDecimal(String value, String fieldName) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			return null;
		}
		try {
			return new BigDecimal(trimmed);
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException(fieldName + " must be a decimal.", ex);
		}
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
