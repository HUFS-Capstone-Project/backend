package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessingJobResultResponse(
		@JsonProperty("job_id")
		String jobId,
		@JsonProperty("status")
		String status,
		@JsonProperty("caption_raw")
		String captionRaw,
		@JsonProperty("instagram_meta")
		InstagramMetaResponse instagramMeta,
		@JsonProperty("resolved_places")
		List<ResolvedPlaceResponse> resolvedPlaces,
		@JsonProperty("error_code")
		String errorCode,
		@JsonProperty("error_message")
		String errorMessage
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record InstagramMetaResponse(
			@JsonProperty("like_count")
			Long likeCount,
			@JsonProperty("comment_count")
			Long commentCount
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ResolvedPlaceResponse(
			@JsonProperty("kakao_place_id")
			String kakaoPlaceId,
			@JsonProperty("place_name")
			String placeName,
			@JsonProperty("address")
			String address,
			@JsonProperty("road_address")
			String roadAddress,
			@JsonProperty("longitude")
			BigDecimal longitude,
			@JsonProperty("latitude")
			BigDecimal latitude,
			@JsonProperty("category_name")
			String categoryName,
			@JsonProperty("category_group_code")
			String categoryGroupCode,
			@JsonProperty("place_url")
			String placeUrl,
			@JsonProperty("phone")
			String phone
	) {
	}
}
