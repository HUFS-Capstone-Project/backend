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
		@JsonProperty("original_url")
		String originalUrl,
		@JsonProperty("canonical_url")
		String canonicalUrl,
		@JsonProperty("crawl_url")
		String crawlUrl,
		@JsonProperty("content")
		ContentResponse content,
		@JsonProperty("link_stats")
		LinkStatsResponse linkStats,
		@JsonProperty("resolved_places")
		List<ResolvedPlaceResponse> resolvedPlaces,
		@JsonProperty("error_code")
		String errorCode,
		@JsonProperty("error_message")
		String errorMessage
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ContentResponse(
			@JsonProperty("source_type")
			String sourceType,
			@JsonProperty("content_text")
			String contentText,
			@JsonProperty("title")
			String title,
			@JsonProperty("description")
			String description,
			@JsonProperty("thumbnail_url")
			String thumbnailUrl,
			@JsonProperty("links")
			List<String> links,
			@JsonProperty("extraction_method")
			String extractionMethod
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record LinkStatsResponse(
			@JsonProperty("like_count")
			Long likeCount,
			@JsonProperty("comment_count")
			Long commentCount,
			@JsonProperty("posted_at")
			String postedAt
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
