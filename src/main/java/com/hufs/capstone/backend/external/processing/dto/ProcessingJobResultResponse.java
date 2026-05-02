package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessingJobResultResponse(
		@JsonProperty("job_id")
		String jobId,
		@JsonProperty("source_url")
		String sourceUrl,
		String source,
		String status,
		String caption,
		@JsonProperty("instagram_meta")
		Map<String, Object> instagramMeta,
		@JsonProperty("extraction_result")
		ExtractionResultResponse extractionResult,
		@JsonProperty("selected_places")
		List<PlaceCandidateResponse> selectedPlaces,
		@JsonProperty("error_message")
		String errorMessage,
		@JsonProperty("updated_at")
		OffsetDateTime updatedAt
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ExtractionResultResponse(
			@JsonProperty("store_name")
			String storeName,
			String address,
			@JsonProperty("store_name_evidence")
			String storeNameEvidence,
			@JsonProperty("address_evidence")
			String addressEvidence,
			String certainty
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record PlaceCandidateResponse(
			@JsonProperty("kakao_place_id")
			String kakaoPlaceId,
			@JsonProperty("place_name")
			String placeName,
			@JsonProperty("category_name")
			String categoryName,
			@JsonProperty("category_group_code")
			String categoryGroupCode,
			@JsonProperty("category_group_name")
			String categoryGroupName,
			String phone,
			@JsonProperty("address_name")
			String addressName,
			@JsonProperty("road_address_name")
			String roadAddressName,
			String x,
			String y,
			@JsonProperty("place_url")
			String placeUrl,
			BigDecimal confidence,
			@JsonProperty("source_keyword")
			String sourceKeyword,
			@JsonProperty("source_sentence")
			String sourceSentence,
			@JsonProperty("raw_candidate")
			String rawCandidate
	) {
	}
}
