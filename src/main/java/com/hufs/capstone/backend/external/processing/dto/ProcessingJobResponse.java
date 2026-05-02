package com.hufs.capstone.backend.external.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessingJobResponse(
		@JsonProperty("job_id")
		String jobId,
		String status,
		@JsonProperty("source_url")
		String sourceUrl,
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
			String sourceUrl,
			String roomId,
			String source,
			String errorCode,
			String errorMessage
	) {
		this(jobId, status, sourceUrl, roomId, source, errorCode, errorMessage, null, null);
	}
}
