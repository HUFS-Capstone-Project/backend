package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessingJobResponse(
		@JsonProperty("job_id")
		String jobId,
		String status,
		@JsonProperty("original_url")
		String originalUrl,
		@JsonProperty("canonical_url")
		String canonicalUrl,
		@JsonProperty("crawl_url")
		String crawlUrl,
		@JsonProperty("room_id")
		String roomId,
		String source,
		@JsonProperty("error_code")
		String errorCode,
		@JsonProperty("error_message")
		String errorMessage,
		@JsonProperty("created_at")
		OffsetDateTime createdAt,
		@JsonProperty("updated_at")
		OffsetDateTime updatedAt
) {

	public ProcessingJobResponse(
			String jobId,
			String status,
			String originalUrl,
			String roomId,
			String source,
			String errorCode,
			String errorMessage
	) {
		this(jobId, status, originalUrl, null, null, roomId, source, errorCode, errorMessage, null, null);
	}
}
